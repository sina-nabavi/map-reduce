package Components;


import Events.InitMessage;
import Ports.EdgePort;
import misc.Edge;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Scanner;


public class App extends ComponentDefinition {

    ArrayList<Edge> edges = new ArrayList<>();
    Map<String,Component> components = new HashMap<String,Component>();

    public App(){
        readTable();
    }

    public static void main(String[] args) throws InterruptedException{
        Kompics.createAndStart(App.class);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            System.exit(1);
        }
        Kompics.shutdown();
//        Kompics.waitForTermination();

    }

    public void readTable(){
        System.out.println("Please enter the number of nodes; Note that this is just for simplification of distance_table gathering");
        Scanner in = new Scanner(System.in);
        int nodesNum = in.nextInt();
        File resourceFile = new File("src/main/java/MST.txt");
        try (Scanner scanner = new Scanner(resourceFile)) {
            int i = 0;
            while (scanner.hasNext()){
                String line = scanner.nextLine();
                if (line.split(",").length > 1){
                    int weight = Integer.parseInt(line.split(",")[1]);
                    String rel = line.split(",")[0];
                    String src = rel.split("-")[0];
                    String dst = rel.split("-")[1];
                    edges.add(new Edge(src,dst,weight));
                }
                i++;
            }
            for(Edge edge:edges){
                if (!components.containsKey(edge.src)){
                    Component c = create(Node.class,new InitMessage(edge.src,findNeighbours(edge.src),nodesNum));
                    components.put(edge.src,c) ;
                }
                if (!components.containsKey(edge.dst)){
                    Component c = create(Node.class,new InitMessage(edge.dst,findNeighbours(edge.dst),nodesNum));
                    components.put(edge.dst,c) ;
                }
                connect(components.get(edge.src).getPositive(EdgePort.class),
                        components.get(edge.dst).getNegative(EdgePort.class), Channel.TWO_WAY);
                connect(components.get(edge.src).getNegative(EdgePort.class),
                        components.get(edge.dst).getPositive(EdgePort.class),Channel.TWO_WAY);
            }





        } catch (IOException e) {
            e.printStackTrace();
        }




    }
    private HashMap<String,Integer> findNeighbours(String node){
        HashMap<String,Integer> nb = new HashMap<String,Integer>();
        for(Edge tr:edges){
            if(tr.src.equalsIgnoreCase(node) && !nb.containsKey(tr.dst)){
                nb.put(tr.dst , tr.weight);
            }
            else if (tr.dst.equalsIgnoreCase(node) && !nb.containsKey(tr.src)){
                nb.put(tr.src , tr.weight);
            }
        }
        return nb;
    }
}
