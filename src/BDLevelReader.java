import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

public class BDLevelReader{
   
    private final int WIDTH = 40;
    private final int HEIGHT = 22;
    private Document doc;
    private BDTile[][] field = new BDTile[WIDTH][HEIGHT];
    private int diamondsNeeded;


    /* Call this method first and only once, passing the level data file name as parameter. */
    public int readLevels(String filename) throws Exception {       //reads the doc as a tree
        File f = new File(filename);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        doc = db.parse(f);
        XPathFactory xpf = XPathFactory.newInstance();
        XPath path = xpf.newXPath();
        int numLevel = Integer.parseInt(path.evaluate("count(levelset/level)", doc));
        return numLevel; // return the number of levels
    }
    
    /* Internal utility method */
    private void readElementsOfType(int level, String name, BDTile value, XPath path) throws Exception {
        int count = Integer.parseInt(path.evaluate("count(levelset/level["+level+"]/" + name + ")", doc));    
        for(int i = 1; i <= count; i++) {
            int x = Integer.parseInt(path.evaluate("levelset/level["+level+"]/" + name + "[" + i + "]/@x", doc));
            int y = Integer.parseInt(path.evaluate("levelset/level["+level+"]/" + name + "[" + i + "]/@y", doc));
            field[x][y] = value;
        }
    }
    
    /* Call this method each time you start a level. Note that level numbers start from 1,
       not from 0. */
    public void setCurrentLevel(int level) throws Exception {
        
        // First, fill the array with DIRT
        for(int x = 0; x < WIDTH; x++) {
            for(int y = 0; y < HEIGHT; y++) {
                field[x][y] = BDTile.DIRT;
            }
        }
        
        Map<String, BDTile> map = new HashMap<String, BDTile>();        
        map.put("wall", BDTile.WALL);
        map.put("rock", BDTile.ROCK);
        map.put("diamond", BDTile.DIAMOND);
        map.put("amoeba", BDTile.AMOEBA);
        map.put("dirt", BDTile.DIRT);
        map.put("empty", BDTile.EMPTY);
        map.put("firefly", BDTile.FIREFLY);
        map.put("butterfly", BDTile.BUTTERFLY);
        map.put("exit", BDTile.EXIT);
        map.put("player", BDTile.PLAYER);
        
        NodeList nlist = doc.getElementsByTagName("levelset");          //from the document we use the method getElementsByTagName, we'll get the root leve of the tree
        Node n = nlist.item(0);                                         //the root element
        nlist = n.getChildNodes();                                      //ask for its children, this is the individual level
        int lvl = 0;                                                    //get through the children, and skip the empty tags
        for(int i = 0; lvl < level && i < nlist.getLength(); i++) {
            n = nlist.item(i);
            if(n.getNodeName().equals("level")) { lvl++; }
        }            
        diamondsNeeded = Integer.parseInt(n.getAttributes().getNamedItem("diamonds").getNodeValue()); //n.getAttributes() get node attributes, how many diamonds are needed in the level
        nlist = n.getChildNodes();
        for(int i = 0; i < nlist.getLength(); i++) {                    //loop through the children
            Node e = nlist.item(i);
            String tag = e.getNodeName();                               //get the tag name 
            if(!map.containsKey(tag)) continue;
            NamedNodeMap attr = e.getAttributes();
            int x = Integer.parseInt(attr.getNamedItem("x").getNodeValue());            
            int y = Integer.parseInt(attr.getNamedItem("y").getNodeValue());
            
            field[x][y] = map.get(tag);                                 //get the elements and put it in the array, associate it with the given tag in the map
        }
    }   
    
    /* Methods for accessing the level data, to be called after you have set the current level */
    public BDTile getTile(int x, int y) { return field[x][y]; }
    public int getDiamondsNeeded() { return diamondsNeeded; }
}