package Client;

import CausalMulticast.CMChannel;
import CausalMulticast.ICausalMulticast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Client implements ICausalMulticast {
    Scanner scanner = new Scanner(System.in);
    CMChannel cm;

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
     * Instancia o CMChannel
     * @throws IOException exceção caso algum problema de IO ocorra
     */
    public void join() throws IOException {
        this.cm = new CMChannel(this);
    }

    /**
     * Método a ser invocado via callback pelo CMChannel
     * Printa a mensagem em rosa para facilitar o debug
     * @param message mensagem recebida do CMChannel
     */
    public void deliver(String message) {
        String RESET_ANSI_COLOR = "\033[0m";
        String PINK_ANSI_COLOR = "\033[38;5;206m";

        System.out.printf("%s%s%s\n", PINK_ANSI_COLOR, message, RESET_ANSI_COLOR);
    }
}
