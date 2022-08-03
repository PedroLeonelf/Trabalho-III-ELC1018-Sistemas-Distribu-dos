package CausalMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Thread para comunicação
 */
@SuppressWarnings("deprecation")
public class IndividualThread extends Thread {

    MulticastSocket socket;
    InetAddress group;
    CausalMulticastChannel cm;

    ArrayList<String> connectedUsers;

    public IndividualThread(CausalMulticastChannel cm) {
        this.socket = cm.socket;
        this.cm = cm;
    }

    @Override
    public void run() {
        try {
            this.connectedUsers = new ArrayList<>();
            this.group = InetAddress.getByName(this.cm.groupIp);
            this.socket.joinGroup(this.group);

            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket resp = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(resp);

                byte[] data = resp.getData();

                ByteArrayInputStream in = new ByteArrayInputStream(data);
                ObjectInputStream is = new ObjectInputStream(in);

                Message message = (Message) is.readObject();
                this.manageMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Realiza o tratamento da mensagem de acordo com o parametro type da mensagem
     * @param message Mensagem a ser tratada
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    public void manageMessage(Message message) throws IOException {

        switch (message.getType()) {
            case Message.JOIN_MSG:
                System.out.printf("[%s]: Entrou no chat!\n", message.getOrigin());
                this.addUserToConnectedUsers(message.getOrigin());
                this.cm.addUserToVectorClock(message.getOrigin());
                this.joinResponse(message.getOrigin());
                break;
            case Message.NORMAL_MSG:
                this.cm.compareAndManageVectorsClock(message);
                break;
            case Message.JOIN_USR_MSG:
                this.addUserToConnectedUsers(message.getOrigin());
                this.cm.addUserToVectorClock(message.getOrigin());
                break;
            default:
                break;
        }
    }

    /**
     * Adiciona o usuário na lista de usuários conectado caso a lista não contenha o mesmo
     * @param origin ip do usuário a ser adicionado
     */
    public void addUserToConnectedUsers(String origin) {
        if (!this.connectedUsers.contains(origin)) {
            this.connectedUsers.add(origin);
        }
    }

    /**
     * Envia uma mensagem do tipo JOIN_USR_MSG para o usuário que conectou
     * @param origin ip do usuário que conectou
     * @throws IOException
     */
    public void joinResponse(String origin) throws IOException {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            try (final DatagramSocket asocket = new DatagramSocket()) {
                asocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = asocket.getLocalAddress().getHostAddress();
            }
            Message message = new Message(Message.JOIN_USR_MSG, "", ip);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(output);
            os.writeObject(message);
            byte[] buffer;
            buffer = output.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(origin), 2020);
            socket.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getConnectedUsers() {
        return this.connectedUsers;
    }
}
