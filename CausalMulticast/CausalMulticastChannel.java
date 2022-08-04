package CausalMulticast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;

public class CausalMulticastChannel {

    ICausalMulticast causalMulticast;
    String ip;
    String groupIp;
    IndividualThread receiveThread;
    MulticastSocket socket;
    private final DatagramSocket unicastSocket;
    private final VectorClock vectorClock;
    // Buffer de mensagens atrasadas que poderão ser enviadas após o comando /sendDelayed
    LinkedHashMap<Message, String> delayedMessages;
    // Buffer com as mensagens bloqueadas enquanto espera a sincronização
    ArrayList<Message> bloquedMessages;

    /**
     * @param causalMulticast o cliente
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    public CausalMulticastChannel(ICausalMulticast causalMulticast) throws IOException {
        this.causalMulticast = causalMulticast;
        this.bloquedMessages = new ArrayList<>();
        this.vectorClock = new VectorClock();
        this.unicastSocket = new DatagramSocket(3030);
        this.socket = new MulticastSocket(2020);
        this.receiveThread = new IndividualThread(this);
        this.delayedMessages = new LinkedHashMap<>();
        this.groupIp = "225.0.0.0";
        receiveThread.start();
        this.join();
    }

    /**
     * Obtem o ip próprio e envia o mesmo para os usuários conectados no grupo multicast
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    protected void join() throws IOException {
        ip = InetAddress.getLocalHost().getHostAddress();
        try (final DatagramSocket asocket = new DatagramSocket()) {
            asocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = asocket.getLocalAddress().getHostAddress();
        }
        Message message = new Message(Message.JOIN_MSG, "", ip);
        sendByMulticastSocket(message);
    }

    /**
     * Envia a mensagem(message) para todos os usuários conectados no grupo multicast
     * @param message mensagem de join
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    private void sendByMulticastSocket(Message message) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(output);
        os.writeObject(message);
        byte[] buffer;
        buffer = output.toByteArray();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.groupIp), 2020);
        this.socket.send(packet);
    }

    /**
     * Adiciona usuário no vectorClock
     * @param origin ip a ser adicionado no vectorClock
     */
    protected void addUserToVectorClock(String origin) {
        vectorClock.createUser(origin);
    }

    /**
     * Compara o vetor da Mensagem(msg) com o vetor interno do CMChannel
     * Caso seja igual, incrementa o vetor interno e entrega a mensagem ao cliente,
     * e verifica se alguma das mensagens guardadas no buffer pode ser entregue.
     * Caso o vectorClock da mensagem seja diferente do vetor interno, a mensagem
     * é guardada no buffer bufferMessages
     * @param msg Mensagem a ser guardada no buffer ou enviada
     */
    protected void compareAndManageVectorsClock(Message msg) {
        boolean isEqual = this.vectorClock.compare(msg.getVectorClock());
        if (isEqual) {
            this.vectorClock.incrementUser(msg.getOrigin());
            this.causalMulticast.deliver("[" + msg.getOrigin() + "]: " + msg.getText());
            this.handleBufferMessages();
        } else {
            System.out.println("Guardando a mensagem: " + msg);
            this.bloquedMessages.add(msg);
        }
        System.out.println("Clocks:" + this.getVectorClock());
        System.out.println("BloquedMessages:" + this.bloquedMessages);
        System.out.println("DelayedMessages:" + this.delayedMessages);
    }

    /**
     * Ao receber uma mensagem, verifica se não é a que estava travando alguma das mensagens guardada no buffer
     */
    private void handleBufferMessages() {
        if (this.bloquedMessages.isEmpty()) {
            return;
        }

        boolean hasFoundMessageInBuffer = true;

        while (hasFoundMessageInBuffer) {
            hasFoundMessageInBuffer = false;
            ArrayList<Message> auxList = new ArrayList<>(this.bloquedMessages);

            for (Message bufferMsg : auxList) {
                hasFoundMessageInBuffer = this.vectorClock.compare(bufferMsg.getVectorClock());
                if (hasFoundMessageInBuffer) {
                    this.vectorClock.incrementUser(bufferMsg.getOrigin());
                    this.bloquedMessages.remove(bufferMsg);
                    this.causalMulticast.deliver("[" + bufferMsg.getOrigin() + "]: " + bufferMsg.getText());
                }
            }
        }
    }

    /**
     * Interage com o cliente (ICausalMulticast) recebendo e tratando a mensagem a ser enviada pelo cliente
     * @param msg Mensagem recebida pelo cliente
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    public void mcsend(String msg) throws IOException {
        String userDecision = "0";
        String yesChars = "sSyY";
        String noChars = "nN";
        String validChars = yesChars + noChars;
        Scanner scanner = new Scanner(System.in);

        if (msg.startsWith("/users")) {
            System.out.println("Usuários: ");
            this.printArray(this.getConnectedUsers());
        } else if (msg.startsWith("/delayList")) {
            System.out.println("Mensagens atrasadas: ");
            System.out.println(Collections.singletonList(this.getbloquedMessages()));
        } else if (msg.startsWith("/clock")) {
            System.out.println("Clocks: ");
            System.out.println(this.getVectorClock());
        } else if (msg.startsWith("/bloquedList")) {
            System.out.println("Buffer:");
            System.out.println(this.getBufferMessages());
        } else if (msg.startsWith("/sendDelayed")) {
            System.out.println("Mensagens atrasadas enviadas!");
            this.sendDelayedMessages();
        } else {
            while (!validChars.contains(userDecision)) {
                System.out.println("Deseja Bloquear alguém? (S/N)");
                userDecision = scanner.nextLine();
            }

            String bannedIp = null;

            if (yesChars.contains(userDecision)) {
                this.printArray(this.getConnectedUsers());
                System.out.println("Digite o numero de quem você quer bloquear:");
                Integer index = Integer.parseInt(scanner.nextLine()) - 1;
                bannedIp = this.getConnectedUsers().get(index);
            }
            this.sendMessage(msg, bannedIp);
        }
    }

    /**
     * A função sendMessage envia via unicast, a mensagem para todos os usuários
     *
     * @param msg mensagem a ser enviada
     * @param bannedIp ip a ser banido
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    protected void sendMessage(String msg, String bannedIp) throws IOException {
        Message message = new Message(Message.NORMAL_MSG, msg, ip);
        message.setVectorClock(this.vectorClock);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(output);
        os.writeObject(message);

        byte[] buffer;
        buffer = output.toByteArray();
        for (String user : receiveThread.getConnectedUsers()) {
            if (user.equals(bannedIp)) {
                System.out.println("[" + user + "]: Bloqueado, adicionarei a mensagem às bloqueadas!");
                this.delayedMessages.put(message, user);
            } else {
                System.out.println("Enviando msg para: " + user);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(user), 2020);
                unicastSocket.send(packet);
            }
        }
    }

    /**
     * Envia as mensagens atrasadas guardadas no buffer bloquedMessages via unicast
     */
    protected void sendDelayedMessages() {
        for (Map.Entry<Message, String> entry : this.delayedMessages.entrySet()) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(output);
                os.writeObject(entry.getKey());

                byte[] buffer;
                buffer = output.toByteArray();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(entry.getValue()), 2020);
                unicastSocket.send(packet);
                System.out.printf("Enviando mensagem {%s} atrasada para %s\n", entry.getKey().getText(), entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.delayedMessages.clear();
    }

    /**
     * Normaliza o print do array
     * @param list Lista a ser printada
     */
    private void printArray(ArrayList<String> list) {
        int index = 0;
        for (String s : list) {
            System.out.println(++index + " - " + s);
        }
    }

    protected Map<String, Integer> getVectorClock() {
        return this.vectorClock.getVector();
    }

    protected ArrayList<Message> getBufferMessages() {
        return bloquedMessages;
    }

    protected LinkedHashMap<Message, String> getbloquedMessages() {
        return this.delayedMessages;
    }

    protected ArrayList<String> getConnectedUsers() {
        return receiveThread.getConnectedUsers();
    }
}
