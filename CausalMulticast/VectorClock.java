package CausalMulticast;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VectorClock implements Serializable {

    /**
     * IP do Usuário, clock do usuário
     */
    private Map<String, Integer> vectorClock;

    public VectorClock() {
        vectorClock = new HashMap<String, Integer>();
    }

    /**
     * Caso o usuario não exista é adicionado o usuário <ip, 0> no vectorClock
     * @param user Usuário a ser criado
     */
    public void createUser(String user) {
        if(!vectorClock.containsKey(user)) {
            vectorClock.put(user, 0);
        }
    }

    /**
     * Caso o usuário exista no vectorClock incrementa-se o clock.
     * @param user usuário a ter clock incrementado
     */
    public void incrementUser(String user) {
        int value = vectorClock.get(user) + 1;
        if (vectorClock.containsKey(user)) {
            vectorClock.put(user, value);
        }
    }

    /**
     * Compara e retorna se os valores de um vectorClock é igual ao de um vectorClock v2
     *
     * Primeiro itera sobre o v2, verificando se todas as entradas existem
     * neste VectorClock, depois itera sobre este VectorClock verificando
     * se todas entradas existem no v2, nas duas iterações é checado se
     * o valor é igual.
     *
     * @param v2 vectorClock a ser comparado
     * @return boolean
     */
    public Boolean compare(VectorClock v2) {
        try {
            for(String user : v2.vectorClock.keySet()) {  // v1 != v2
                if(!this.vectorClock.get(user).equals(v2.vectorClock.get(user))) {
                    return false;
                }
            }

            for(String user : this.vectorClock.keySet()) {
                if(!v2.vectorClock.containsKey(user)) { // v2 != v1
                    return false;
                }
            }
        }
        catch(Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            return false;
        }

        return true;
    }

    public void setVector(Map<String, Integer> vector) {
        for(String key : vector.keySet()) {
            vectorClock.put(key, vector.get(key)); //vectorClock <- vector
        }
    }

    public Map<String, Integer> getVector() {
        return vectorClock;
    }

    @Override
    public String toString() {
        String string = "[ ";

        for(String key : vectorClock.keySet()) {
            string += key + ":" + vectorClock.get(key) + " | ";
        }
        string += "]";

        return string;
    }
}
