import javax.swing.*;        
import java.util.*;

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;

  // Custom
  private HashMap<Integer,Integer> forwarding = new HashMap<Integer,Integer>();  // Hash con los nodos a los que se redirecciona. Ej: <x,y> si quiero ir a x debo reenviar a y 
  private HashMap<Integer,Integer> costs = new HashMap<Integer,Integer>();       // Hash con los costos. Ej: <x,y> si quiero ir a x me cuesta y

  private Boolean envenenada = false;

  //--------------------------------------------------
  /*
  * Funcion constructor del RouterNode
  * @param ID     Integer         identificador del RouterNode
  * @param sim    RouterSimulator Instancia del "Programa principal". Se utiliza para la comunicacion entre RouterNodes ( a traves de la funcion toLayer2 )
  * @param costs  HashMap         Contiene la tabla de costos inicial para el nodo en particular
  */
  public RouterNode(int ID, RouterSimulator sim, HashMap<Integer,Integer> costs) {
    myID = ID;
    this.sim = sim;
    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");

    this.costs = costs.clone();
    //Inicializo la tabla de forwarding 
    Iterator it = costs.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry pair = (Map.Entry)it.next();
        System.out.println(pair.getKey() + " = " + pair.getValue());
        it.remove(); // avoids a ConcurrentModificationException
    }

  }

  public Boolean getEnvenenada(){ 
    return this.envenenada;
  }

  public void setEnvenenada(Boolean envenenada){
    this.envenenada = envenenada;
  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {

  }
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }
  

  //--------------------------------------------------
  public void printDistanceTable() {
	  myGUI.println("Current table for " + myID +
			"  at time " + sim.getClocktime());

  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
  }

}
