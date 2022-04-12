package qtx.webSocket;

import java.io.IOException;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

// Se agregó al build path la ruta de apache juli: C:\apache-tomcat-8.0.14\bin\tomcat-juli.jar
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

@ServerEndpoint(value = "/websocket/chat")
public class ConexionClienteChat {

    private static final Log log = LogFactory.getLog(ConexionClienteChat.class);

    private static final String PREFIJO_PARTICIPANTE = "Invitado # ";
    
    // support lock-free thread-safe programming on single variables,
    // provide access and updates to a single variable of the corresponding type
    private static final AtomicInteger idConexionI = new AtomicInteger(0);
    
    
    private static final Set<ConexionClienteChat> conexiones =
    		
    		// all mutative operations (add, set, and so on) are implemented by making a fresh copy of the underlying array.
    		/*
    		 * Esto es generalmente demasiado costoso, pero puede ser más eficiente que las alternativas 
    		 * cuando las operaciones de recorrido superan con creces a las mutaciones, y es útil cuando 
    		 * no se puede o no se desea sincronizar los recorridos, pero se necesita evitar la interferencia 
    		 * entre hilos concurrentes. 
    		 * El método del iterador de estilo "instantánea" utiliza una referencia al estado del  arreglo en el punto en 
    		 * que se creó el iterador. Este arreglo nunca cambia durante la vida útil del iterador, por lo que la 
    		 * interferencia es imposible y se garantiza que el iterador no lanzará ConcurrentModificationException.
    		 * 
    		 * */ 
            new CopyOnWriteArraySet<>();

    private final String nickname;
    private Session sesionWebsocket;

    public ConexionClienteChat() {
        nickname = PREFIJO_PARTICIPANTE + idConexionI.getAndIncrement();
    }


    @OnOpen
    public void iniciarConexion(Session session) {
        this.sesionWebsocket = session;
        conexiones.add(this);
        String message = String.format("* El %s %s", nickname, "se ha unido al chat.");
        publicarGlobalmente(message);
    }


    @OnClose
    public void terminarConexion() {
        conexiones.remove(this);
        String message = String.format("* %s %s",
                nickname, "se ha desconectado.");
        publicarGlobalmente(message);
    }


    @OnMessage
    public void atenderMensaje(String message) {
        String mensajeConID = String.format("%s: %s",
                nickname, message);
        publicarGlobalmente(mensajeConID);
    }




    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("Chat Error: " + t.toString(), t);
    }


    private static void publicarGlobalmente(String msg) {
        for (ConexionClienteChat conexionI : conexiones) {
            try {
                synchronized (conexionI) {               	
                   if( conexionI.sesionWebsocket.isOpen() )
                	   conexionI.sesionWebsocket.getBasicRemote()
                                                .sendText(msg);
                }
            } catch (IOException e) {
                log.debug("Error en el Chat: Falló el envío del mensaje al cliente", e);
            }
        }
    }
}