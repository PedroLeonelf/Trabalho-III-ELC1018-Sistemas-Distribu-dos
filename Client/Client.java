package Client;

import CausalMulticast.CausalMulticastChannel;
import CausalMulticast.ICausalMulticast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Client implements ICausalMulticast {
    Scanner scanner = new Scanner(System.in);
    CausalMulticastChannel cm;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        String message;

        client.join();

        while(true) {
            message = client.scanner.nextLine();
            client.cm.mcsend(message);
        }
    }

    /**
     * Instancia o canal do multicast
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    public void join() throws IOException {
        this.cm = new CausalMulticastChannel(this);
    }

    /**
     * Método a ser invocado via callback pelo canal do multicast
     * @param message mensagem recebida pelo canal do multicast
     */
    public void deliver(String message) {
        System.out.printf("%s\n", message);
    }
}
