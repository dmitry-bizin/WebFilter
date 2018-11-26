package proxy.http;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Поток на запуск HTTP прокси-сервера
 */
public class HttpProxyThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(HttpProxyThread.class);
    private ExecutorService executorService;
    private ServerSocket serverSocket;

    //todo: разобраться с executorService
    // сделать его общим на два прокси и при изменении кол-ва потоков текущий завершать корректно!
    public HttpProxyThread(ServerSocket serverSocket, int threadsCount) {
        this.executorService = Executors.newFixedThreadPool(threadsCount);
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while (true) {
            if (serverSocket == null || serverSocket.isClosed()) {
                break;
            } else {
                try {
                    executorService.execute(new HttpClientProxyThread(serverSocket.accept()));
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

}