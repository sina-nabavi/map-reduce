package Components;
import Events.*;
import Ports.EdgePort;
import misc.Edge;
import misc.TableRow;
import se.sics.kompics.*;
import se.sics.kompics.Component;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class Node extends ComponentDefinition {

    public static ArrayList<String> mstEdges = new ArrayList<String>();

    Positive<EdgePort> recievePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    String nodeName;
    HashMap<String,Integer> neighbours = new HashMap<>();
    HashMap<String, String> echoParents = new HashMap<>();
    HashMap<String, String> waveParents = new HashMap<>();
    HashMap<String, Integer> distanceTable = new HashMap<>();
    HashMap<String, Integer> neighboursRepliedToEcho = new HashMap<>();
    HashMap<String, Integer> neighboursRepliedToWave = new HashMap<>();
    HashMap<String,Integer> leavesInSubtree = new HashMap<>();
    HashMap<String,Integer> mapsSentOnChannel = new HashMap<>();
    HashMap<String, Integer> reducesReceivedOnChannel = new HashMap<>();
    HashMap<String,Integer> reducedMaps = new HashMap<>();
    int leavesNum;
    ArrayList<TableRow> route_table = new ArrayList<>();
    HashMap<String, Boolean> seenChannel = new HashMap<>();
    int averageDistance;
    int isRoot;
    int nodesNum;
    boolean isLeaf;
    int minimumDistOtherNodes;
    String tarryParrent;
    boolean tarrySeenNode;
    ArrayList<String> words;

    ArrayList<Integer> splitter(int x, int n){
        ArrayList chunks = new ArrayList<String>();
        if(x % n == 0){
            for(int i = 0; i < n; i++){
                chunks.add(x/n);
            }
        }

        else
        {
            int zp = n - (x % n);
            int pp = x/n;
            for(int i=0;i<n;i++)
            {

                if(i>= zp)
                    chunks.add(pp + 1);
                else
                    chunks.add(pp);
            }
        }
        return chunks;
    }
    ArrayList<String> readData(String fileName) {
        Scanner in = new Scanner(System.in);
        ArrayList dictionary = new ArrayList<String>();
        File resourceFile = new File("src/main/java/" + fileName);
        try (Scanner scanner = new Scanner(resourceFile)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                line = line.replaceAll(","," , ");
                line = line.replaceAll("\\."," \\. ");
                line = line.replaceAll("-", " - ");
                String[] result = line.split(" ");
                for(String word: result){
                    if(word.matches("([a-zA-Z]+)")) {
                        dictionary.add(word);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dictionary;
    }
    Handler reducerHandler = new Handler<ReducerMessage>(){
        @Override
        public void handle(ReducerMessage event){
            if (event.dst.equalsIgnoreCase(nodeName)) {
                System.out.println("REDUCE MESSAGE from " + event.src + " to " + event.dst);
                if (reducesReceivedOnChannel.containsKey(event.src)) {
                    int last = reducesReceivedOnChannel.get(event.src);
                    reducesReceivedOnChannel.put(event.src, last + 1);
                }
                else {
                    reducesReceivedOnChannel.put(event.src, 1);
                }
                for (Map.Entry<String, Integer> pair : (event.words).entrySet()) {
                    String word = pair.getKey();
                    int number = pair.getValue();
                    if (reducedMaps.containsKey(word)){
                        int last = reducedMaps.get(word);
                        reducedMaps.put(word, last + number);
                    }

                    else {
                        reducedMaps.put(word, number);
                    }
                }


                boolean seenAll = true;
                if (isRoot != 1) {
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(tarryParrent))
                            continue;
                        if(reducesReceivedOnChannel.containsKey(entry.getKey()) == false)
                            continue;
                        if (reducesReceivedOnChannel.get(entry.getKey()) < leavesInSubtree.get(entry.getKey()))
                            seenAll = false;
                    }
                    if (seenAll) {
                        trigger(new ReducerMessage(nodeName, tarryParrent, reducedMaps), sendPort);
                    }
                }
                if (isRoot == 1) {
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if(reducesReceivedOnChannel.containsKey(entry.getKey()) == false){
                            seenAll = false;
                            break;
                        }
                        if (reducesReceivedOnChannel.get(entry.getKey()) < leavesInSubtree.get(entry.getKey())) {
                            seenAll = false;
                            break;
                        }
                    }
                    if (seenAll) {
                        System.out.println("I AM ROOT " + nodeName + " AND I HAVE REDUCED ALL THE WORDS IN FILE");
                        try (FileWriter file = new FileWriter("src/main/java/" + "wordlist" + ".txt")) {
                            BufferedWriter writer = new BufferedWriter(file);

                            for (Map.Entry<String, Integer> word : reducedMaps.entrySet()) {
                                String line = word.getKey();
                                line += ":";
                                line += word.getValue();
                                writer.append(line);
                                writer.newLine();

                            }
                            writer.close();
                            file.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    };
    Handler mapperHandler = new Handler<MapperMessage>(){
      @Override
      public void handle(MapperMessage event){
          if (event.dst.equalsIgnoreCase(nodeName)) {
              System.out.println("MAP MESSAGE from " + event.src + " to " + event.dst);
              if (isLeaf) {
                  HashMap<String, Integer> chunkWords = new HashMap<>();
                  ArrayList<String> dictionary = event.dictionary;
                  for (int i = event.begin; i <= event.end; i++) {
                      if(dictionary.get(i) == null)
                          continue;
                      if (chunkWords.containsKey(dictionary.get(i))) {
                          int lastCount = chunkWords.get(dictionary.get(i));
                          chunkWords.put(dictionary.get(i), lastCount + 1);
                      } else {
                          chunkWords.put(dictionary.get(i), 1);
                      }
                  }
                  trigger(new ReducerMessage(nodeName, tarryParrent, chunkWords), sendPort);
              } else {
                  for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                      if (entry.getKey().equalsIgnoreCase(tarryParrent))
                          continue;
                      if (mapsSentOnChannel.get(entry.getKey()) != null && (mapsSentOnChannel.get(entry.getKey()) == leavesInSubtree.get(entry.getKey())))
                          continue;
                      if(mapsSentOnChannel.get(entry.getKey()) == null)
                          mapsSentOnChannel.put(entry.getKey(), 0);
                      int sentOnChannel = mapsSentOnChannel.get(entry.getKey());
                      sentOnChannel += 1;
                      mapsSentOnChannel.put(entry.getKey(), sentOnChannel);
                      trigger(new MapperMessage(event.dst, entry.getKey(), event.dictionary, event.begin, event.end), sendPort);
                  }
              }
          }
      }
    };
    Handler leafNumCountHandler = new Handler <LeafNumCounterMessage>(){
        @Override
        public void handle(LeafNumCounterMessage event) {
            if (event.dst.equalsIgnoreCase(nodeName)) {
                System.out.println("LEAF COUNTER MESSAGE from " + event.src + " to " + event.dst + " with " + event.token + " found leaves");
                if(isLeaf){
                    tarryParrent = event.src;
                    trigger(new LeafNumCounterMessage(nodeName, event.src, 1),sendPort);
                }
                else{
                    if(isRoot != 1) {
                        if (tarrySeenNode == false) {
                            tarrySeenNode = true;
                            tarryParrent = event.src;
                        }
                    }
                    boolean seenAll = true;
                    if(tarryParrent == null || event.src != tarryParrent) {
                        leavesNum += event.token;
                        leavesInSubtree.put(event.src, event.token);
                    }
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(tarryParrent))
                            continue;
                        if(seenChannel.get(entry.getKey()) != null)
                            continue;
                        trigger(new LeafNumCounterMessage(nodeName, entry.getKey(), 0), sendPort);
                        seenChannel.put(entry.getKey(), true);
                        seenAll = false;
                        break;
                    }
                    if(seenAll){
                        if(isRoot != 1){
                            trigger(new LeafNumCounterMessage(nodeName, tarryParrent, leavesNum),sendPort);
                        }
                        if(isRoot == 1){
                            System.out.println("I AM ROOT " + nodeName + " AND I FOUND ALL THE LEAVES WITH COUNT OF " + leavesNum);
                            ArrayList<Integer> chunks = splitter(words.size(), leavesNum);
                            int index = 0;
                            int iterator = 0;
                            for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                                System.out.println("THERE ARE " + leavesInSubtree.get(entry.getKey()) + " LEAF/LEAVES ON SUBTREE " + entry.getKey());
                            }
                            for(int i = 0; i < chunks.size(); i++){
                                System.out.println("THIS IS THE CHUNK SIZE OF INDEX: " + i + " " + chunks.get(i));
                            }
                            for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                                for(int i = 0 ; i < leavesInSubtree.get(entry.getKey()); i++){
                                    trigger(new MapperMessage(nodeName, entry.getKey(),words, index, index + chunks.get(iterator) - 1),sendPort);
                                    index += chunks.get(iterator);
                                    iterator += 1;
                                }
                            }
                        }
                    }
                }
            }
        }
    };
    Handler waveHandler = new Handler<WaveMessage>(){
        @Override
        public void handle(WaveMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                if (event.dist < minimumDistOtherNodes)
                    minimumDistOtherNodes = event.dist;
                System.out.println("WAVE MESSAGE WITH ID: " + event.waveId + " from " + event.src + " to " + event.dst + " Average Distance " + event.dist);
                if (waveParents.containsKey(event.waveId) == false && (event.waveId.equalsIgnoreCase(nodeName) == false)) {
                    waveParents.put(event.waveId, event.src);
                    neighboursRepliedToWave.put(event.waveId, 0);
                    if (neighbours.size() == 1) {
                        trigger(new WaveMessage(nodeName, waveParents.get(event.waveId), event.dist, event.waveId), sendPort);
                    } else {
                        for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(waveParents.get(event.waveId)))
                                continue;
                            trigger(new WaveMessage(nodeName, entry.getKey(), event.dist, event.waveId), sendPort);
                        }
                    }
                } else {
                    int addedDistance = event.dist;
                    int seen = neighboursRepliedToWave.get(event.waveId);
                    seen += 1;
                    neighboursRepliedToWave.put(event.waveId, seen);
                    int oldDistance = distanceTable.get(event.waveId);
                    distanceTable.put(event.waveId, oldDistance + addedDistance);
                    if (neighboursRepliedToWave.get(event.waveId) == neighbours.size() && event.waveId.equalsIgnoreCase(nodeName)) {
                        if (minimumDistOtherNodes >= averageDistance) {
                            isRoot = 1;
                            System.out.println("I BECAME ROOT WITH ID: " + nodeName);
                            words = readData("CA-text-file.txt");
                            Map.Entry<String, Integer> entry = neighbours.entrySet().iterator().next();
                            seenChannel.put(entry.getKey(), true);
                            trigger(new LeafNumCounterMessage(nodeName, entry.getKey(), 0),sendPort);
                        }
                    }
                    if (neighboursRepliedToWave.get(event.waveId) == (neighbours.size() - 1)) {
                        trigger(new WaveMessage(nodeName, waveParents.get(event.waveId), event.dist, event.waveId), sendPort);
                    }
                }

            }
        }
    };
    Handler echoHandler = new Handler<EchoMessage>(){
      @Override
      public void handle(EchoMessage event){
          if(nodeName.equalsIgnoreCase(event.dst)){
              System.out.println("ECHO MESSAGE WITH ID: " + event.echoId + " from " + event.src + " to " + event.dst + " dist " + event.dist);
              if(echoParents.containsKey(event.echoId) == false && (event.echoId.equalsIgnoreCase(nodeName) == false)){
                  echoParents.put(event.echoId, event.src);
                  distanceTable.put(event.echoId, event.dist + neighbours.get(event.src));
                  neighboursRepliedToEcho.put(event.echoId, 0);
                  if(neighbours.size() == 1){
                      trigger(new EchoMessage(nodeName, echoParents.get(event.echoId), distanceTable.get(event.echoId), event.echoId), sendPort);
                  }
                  else {
                      for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                          if (entry.getKey().equalsIgnoreCase(echoParents.get(event.echoId)))
                              continue;
                          trigger(new EchoMessage(nodeName, entry.getKey(), distanceTable.get(event.echoId), event.echoId), sendPort);
                      }
                  }
              }

              else{
                int addedDistance = event.dist;
                int seen = neighboursRepliedToEcho.get(event.echoId);
                seen += 1;
                neighboursRepliedToEcho.put(event.echoId, seen);
                int oldDistance = distanceTable.get(event.echoId);
                distanceTable.put(event.echoId, oldDistance + addedDistance);
                if(neighboursRepliedToEcho.get(event.echoId) == neighbours.size() && event.echoId.equalsIgnoreCase(nodeName)){
                    averageDistance = distanceTable.get(nodeName);
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                        trigger(new WaveMessage(nodeName, entry.getKey(),averageDistance,nodeName),sendPort);
                    }
                }

                if(neighboursRepliedToEcho.get(event.echoId) == (neighbours.size() - 1)){
                    trigger(new EchoMessage(nodeName, echoParents.get(event.echoId), distanceTable.get(event.echoId), event.echoId), sendPort);
                }
              }
          }
      }

    };

    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            neighboursRepliedToEcho.put(nodeName, 0);
            neighboursRepliedToWave.put(nodeName, 0);
            distanceTable.put(nodeName, 0);
            for (Map.Entry<String, Integer> entry : neighbours.entrySet()) {
                trigger(new EchoMessage(nodeName, entry.getKey(),0, nodeName),sendPort);
            }
        }
    };

    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        System.out.println("initNode :" + initMessage.nodeName);
        this.isRoot = 0;
        this.neighbours = initMessage.neighbours;
        this.averageDistance = 0;
        this.nodesNum = initMessage.nodesNum;
        this.minimumDistOtherNodes = 100000;
        this.tarrySeenNode = false;
        this.leavesNum = 0;
        if(neighbours.size() == 1)
            isLeaf = true;
        subscribe(startHandler, control);
        subscribe(echoHandler, recievePort);
        subscribe(waveHandler, recievePort);
        subscribe(leafNumCountHandler, recievePort);
        subscribe(mapperHandler, recievePort);
        subscribe(reducerHandler, recievePort);
    }


}

