import java.util.*;

public class RouterNode {
    private int myID;
    private GuiTextArea myGUI;
    private RouterSimulator sim;

    // Custom
    // Utilizamos HashMap simplemente porque los costos vienen de esta manera
    private HashMap<Integer,Integer> forwarding = new HashMap<Integer,Integer>();  // Hash con los nodos a los que se redirecciona. Ej: <x,y> si quiero ir a x debo reenviar a y 
    private HashMap<Integer,Integer> costs      = new HashMap<Integer,Integer>();       // Hash con los costos. Ej: <x,y> si quiero ir a x me cuesta y (Solo conexiones directas)
    private HashMap<Integer,Integer> distancias  = new HashMap<Integer,Integer>();       // Hash con las distancias. Ej: <x,y> si quiero ir a x me cuesta y

    private Boolean envenenada = true; // Flag de reversa envenenada

    private int spaces  = 15;   // Variable utilizada para el formateo

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

        this.costs      = (HashMap<Integer, Integer>) costs.clone();
        this.distancias  = (HashMap<Integer, Integer>) costs.clone();

        //Por como esta creado el HashMap sabemos que todos de 0 hasta sim.NUM_NODES -1 
        //tenemos datos por lo tanto nos podemos dar la libertad de realizar un for para obtener los datos

        for(int x = 0; x < RouterSimulator.NUM_NODES;x++){            
            if( this.costs.containsKey(x) ){ // Si el costo esta setteado
                this.forwarding.put(x,x);       // Agregamos el valor a forwarding
                //Envio mi tabla a este vecino
                
            }else{                
                this.forwarding.put(x,x != this.myID? RouterSimulator.INFINITY : x );    // Si es null quiere decir que no tiene link directo
                this.costs.put(x,x != this.myID? RouterSimulator.INFINITY : 0 );         // Inicializo los costos
                this.distancias.put(x,x != this.myID? RouterSimulator.INFINITY : 0 );         // Inicializo los costos                
            }
        }
        
        this.printDistanceTable();
        
        for(int x = 0; x < RouterSimulator.NUM_NODES; x++){
            if(x != this.myID && this.costs.get(x) != RouterSimulator.INFINITY){ // Si es mi vecino ( costo directo distinto de infinito ) y no soy yo mismo
                RouterPacket routerPacket = new RouterPacket(this.myID,x,this.distancias);   // Creo un reouterPacket
                this.sendUpdate(routerPacket); // Envio los datos                 
            }
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
        Boolean needBroadcast               = false;
        HashMap<Integer,Integer> mincost    = pkt.mincost; // Obtengo el arbol de costo minimo 
        int source                          = pkt.sourceid;
        int sourceCost                      = this.costs.get(source);   // Obtengo el costo de ir hasta el nodo que envia el arbol
        for(int x = 0; x < mincost.size(); x++){    // Recorro el arbol minimo
            // Utilizando el algoritmo de Bellman-Ford
            if( mincost.containsKey(x) ){
                if( sourceCost + mincost.get(x) < this.distancias.get(x) ){      // Si c(this,source) + c(source,x) < c(this,x) | Si me queda mas corto ir por el nodo source que mi ruta anterior                
                    this.forwarding.put(x,source);                             // En mi lista de forwarding coloco que para ir a x nos redirijimos a source
                    this.distancias.put(x,sourceCost + mincost.get(x));     // Actualizo mi arbol de costos con c(this,x) = c(this,source) + c(source,x)
                    needBroadcast = true;
                }
            }   
            // Para la actualizacion de Links
            // Si me redirigo al nodo source para ir a x
            // Y el c(source,x) + d(this,x) > c(this,x)
            // Entonces nos queda mas facil ir directamente a x desde this
            if( this.forwarding.get(x) == source && mincost.get(x) + this.distancias.get(source) >  this.costs.get(x) ){  
                this.distancias.put(x,this.costs.get(x)); // Actualizo la distancia del link a el costo directo entre this y source del packet
                this.forwarding.put(x, x);                // Actualizo el forwarding
                needBroadcast = true;
            }
        }
        if(!needBroadcast){            
            // Si no necesita el broadcast verifico si tengo alguna distancia en infinito
            for(int x = 0; x < RouterSimulator.NUM_NODES;x++){
                if(this.distancias.get(x) == RouterSimulator.INFINITY){ // Si hay alguna distancia en infinito 
                    needBroadcast = true;   // Coloco en true para realizar el broadcast
                }
            }
        }
        
        if( needBroadcast ){
            for(int x = 0; x < RouterSimulator.NUM_NODES; x++){ // Hago el broadcast de mi actualizacion de costos
                if(this.costs.get(x) != RouterSimulator.INFINITY && this.myID != x){ 
                    RouterPacket routerPacket = new RouterPacket(this.myID,x,this.distancias);   // Creo un reouterPacket
                    this.sendUpdate(routerPacket);
                }
            }
        }
        this.printDistanceTable(); // Imprimo mi tabla de distancias
    }
  

    //--------------------------------------------------
    private void sendUpdate(RouterPacket pkt) {
        // ! Utilizar la reversa envenenada no funciona siempre
        // Si el camino tiene un tamaño mayor a 3 no funciona 
        if ( this.getEnvenenada() ){    // Si se utiliza la reversa envenenada
            for (int x = 0; x < RouterSimulator.NUM_NODES; x++ ) {  
                if (this.forwarding.get(x) == pkt.destid){   // Si llego a x a traves de el destino del paquete
                    pkt.mincost.put(x, RouterSimulator.INFINITY ); // Le mando infinito 
                }
            }
        }
        sim.toLayer2(pkt);

    }
  

  //--------------------------------------------------
    public void printDistanceTable() {
        myGUI.println("Current table for " + myID +
        "  at time " + sim.getClocktime());


        myGUI.println("La reversa envenenada "+(this.getEnvenenada()? "esta activa" : "desactivada"));

        /* Pasamos a imprimir la tabla de esta manera
        | 0 . . . . . . n
        costos  | x . . . . . . x
        forward | 
        */
        String nodos    = F.format("nodo: "+this.myID,this.spaces)+"|";
        String dist     = F.format("distancia",this.spaces)+"|";
        String costos   = F.format("costos",this.spaces)+"|";
        String forward  = F.format("forward",this.spaces)+"|";
        String spaces   = "";

        for(int x = 0; x < RouterSimulator.NUM_NODES; x++){
            nodos   += F.format(x , this.spaces  );
            dist    += F.format(this.distancias.get(x) , this.spaces  );
            costos  += F.format(this.costs.get(x) , this.spaces  );
            forward += F.format(this.forwarding.get(x) , this.spaces  );
        }
        for(int y = 0; y < nodos.length();y++){
            spaces += "-";
        }

        myGUI.println(nodos);
        myGUI.println(spaces);  
        myGUI.println(dist);
        myGUI.println(costos);
        myGUI.println(forward);
        myGUI.println();
        myGUI.println();

    }

    //--------------------------------------------------
    public void updateLinkCost(int dest, int newcost) {
        // Funcion que actualiza el costo del enlace
        Boolean needBroadcast = false;
        int previousCost = this.costs.get(dest);
        this.costs.put(dest,newcost);   // Actualizo los costos para dicho nodo
        for(int x = 0; x < RouterSimulator.NUM_NODES; x++){ // Recorro los nodos            
            if( this.forwarding.get(x) == dest ){           // Si alguno de la redireccion es el afectado
                // Actualizo la distancia
                this.distancias.put(x,  this.distancias.get(x) + (newcost - previousCost)); // La nueva distancia es la anterior mas la diferencia entre el nuevo costo y el anterior
                needBroadcast = true;                  
            }
        }
        if(needBroadcast){
            for(int x = 0; x < RouterSimulator.NUM_NODES; x++){ // Hago el broadcast de mi actualizacion de costos
                if(this.costs.get(x) != RouterSimulator.INFINITY && this.myID != x){ 
                    RouterPacket routerPacket = new RouterPacket(this.myID,x,this.distancias);   // Creo un reouterPacket
                    this.sendUpdate(routerPacket);
                }
            }
        }
        this.printDistanceTable(); // Imprimo mi tabla de distancias
        
    }

}
