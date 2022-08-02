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
 * Thread responsável por receber e tratar as mensagens via multicast
 */
@SuppressWarnings("deprecation")
public class ReceiveThread extends Thread {

    MulticastSocket socket;
    InetAddress group;
    CMChannel cm;

    ArrayList<String> connectedUsers;

    public ReceiveThread(CMChannel cm) {
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
                byte[] buf = new byte[1000];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
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
        String RESET_ANSI_COLOR = "\033[0m";
        String GREEN_ANSI_COLOR = "\u001B[32m";
        switch (message.getType()) {
            case Message.JOIN_MSG:
                System.out.printf("%s[%s]: Entrou no chat!%s\n", GREEN_ANSI_COLOR, message.getFrom(), RESET_ANSI_COLOR);
                this.addUserToConnectedUsers(message.getFrom());
                this.cm.addUserToVectorClock(message.getFrom());
                this.joinResponse(message.getFrom());
                break;
            case Message.NORMAL_MSG:
                this.cm.compareAndManageVectorsClock(message);
                break;
            case Message.JOIN_RES:
                this.addUserToConnectedUsers(message.getFrom());
                this.cm.addUserToVectorClock(message.getFrom());
                break;
            default:
                break;
        }
    }

    /**
     * Adiciona o usuário na lista de usuários conectado caso a lista não contenha o mesmo
     * @param from ip do usuário a ser adicionado
     */
    public void addUserToConnectedUsers(String from) {
        if (!this.connectedUsers.contains(from)) {
            this.connectedUsers.add(from);
        }
    }

    /**
     * Envia uma mensagem do tipo JOIN_RES para o usuário que conectou
     * @param from ip do usuário que conectou
     * @throws IOException
     */
    public void joinResponse(String from) throws IOException {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            try (final DatagramSocket asocket = new DatagramSocket()) {
                asocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = asocket.getLocalAddress().getHostAddress();
            }
            Message message = new Message(Message.JOIN_RES, "", ip);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(output);
            os.writeObject(message);
            byte[] buffer;
            buffer = output.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(from), 2020);
            socket.send(packet);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getConnectedUsers() {
        return this.connectedUsers;
    }
}
