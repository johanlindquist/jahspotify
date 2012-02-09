package jahspotify.web;

import java.io.IOException;
import java.net.*;
import javax.annotation.*;

import com.google.gson.Gson;
import jahspotify.services.QueueManager;
import jahspotify.web.system.ServerIdentity;
import org.apache.commons.logging.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Johan Lindquist
 */
@Service
@Lazy(value = false)
public class ServerBroadcaster
{
    private Log _log = LogFactory.getLog(QueueManager.class);

    @Value(value = "${jahspotify.server.discovery-broadcast-interval}")
    private int _interval = 5000;

    @Value(value = "${jahspotify.server.discovery-broadcast-port}")
    private int _port = 9764;
    @Value(value = "${jahspotify.server.discovery-broadcast-address}")
    private String _address = "224.0.0.1";

    @Value(value = "${jahspotify.server.id}")
    private String _serverId = "JahSpotify";

    @Value(value = "${jahspotify.web.port}")
    private int _webPort = 8080;

    @Value(value = "${jahspotify.web.api-version}")
    private String _apiVersion = "0.1";
    private boolean _keepRunning = true;

    private static long _upSince = System.currentTimeMillis();
    
    public final Thread _senderThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                InetAddress group = InetAddress.getByName(_address);
                DatagramSocket s = new DatagramSocket(_port);
                s.setReuseAddress(true);

                ServerIdentity serverIdentity = new ServerIdentity(_serverId, _apiVersion, _webPort, "MASTER", _upSince);

                while (_keepRunning)
                {
                    try
                    {
                        String msg = new Gson().toJson(serverIdentity);

                        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, _port);
                        s.send(hi);
                        Thread.sleep(_interval);
                    }
                    catch (InterruptedException e)
                    {
                        // Ignore
                    }
                    catch (Exception e)
                    {
                        _log.error("Error while sending broadcast discovery message: " + e.getMessage());
                    }
                }

                s.close();

            }
            catch (Exception e)
            {
                _log.error("Error while setting up sender thread: " + e.getMessage(), e);
            }
        }
    });

    @PreDestroy
    private void shutdown()
    {
        _keepRunning = false;
        _senderThread.interrupt();
    }

    @PostConstruct
    private void initialize()
    {

        _senderThread.setDaemon(true);
        _senderThread.setPriority(Thread.MIN_PRIORITY);
        _senderThread.start();
    }
}
