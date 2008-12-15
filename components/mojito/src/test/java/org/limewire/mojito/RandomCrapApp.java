package org.limewire.mojito;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Random;

/**
 * A small app to test the robustness of the IO component.
 * It sends random data over UDP to the specified host.
 */
public class RandomCrapApp implements Runnable {
    
    private String host;
    private int port;
    
    public RandomCrapApp(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void run() {
        try {
            
            InetSocketAddress addr = new InetSocketAddress(host, port);
            DatagramSocket socket = new DatagramSocket(5000);
            
            Random random = new Random();
            while(true) {
                byte[] data = new byte[random.nextInt(1024) /*Math.max(random.nextInt(1024), 24)*/];
                random.nextBytes(data);
                
                /*if (data.length > 16) {
                    data[16] = DHTMessage.F_DHT_MESSAGE;
                }
                
                if (data.length > 23) {
                    data[23] = 0x06;
                }*/
                
                DatagramPacket packet = new DatagramPacket(data, data.length, addr);
                socket.send(packet);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        
        new RandomCrapApp(host, port).run();
    }
}
