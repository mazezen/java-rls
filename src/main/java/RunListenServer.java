import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RunListenServer {

    private static String serverName;
    private static String serverBinary;
    private static int serverPort;
    private  static int CheckInterval = 30;

    private static Process serverProcess;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("miss arguments");
            return;
        }

        serverName = args[0]; // server name
        serverBinary = args[1]; // server binary path
        serverPort = Integer.parseInt(args[2]); // server port

        System.out.printf("[%s] 监控服务 %s 开始运行...\\n", LocalDateTime.now(), serverName);

        if (!startService()) {
            System.err.printf("[%s] 启动服务 [%s] 失败, 尝试继续重启\n", LocalDateTime.now(), serverName);
            return;
        }


        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (!isRunning(serverPort)) {
                System.out.printf("[%s] 监测服务 [%s] 已挂掉,尝试重新启动...\n", LocalDateTime.now(), serverName);
                if (!startService()) {
                    System.err.printf("[%s] 重启服务 %s 失败\n", LocalDateTime.now(), serverName);
                } else {
                    System.out.printf("[%s] 服务 %s 已成功重启\n", LocalDateTime.now(), serverName);
                }
            } else {
                System.out.printf("[%s] 服务 %s 正常运行中\n", LocalDateTime.now(), serverName);
            }
        }, 0, CheckInterval, TimeUnit.SECONDS);


    }


    private static boolean startService() {
        stopService();

        ProcessBuilder processBuilder = new ProcessBuilder(serverBinary);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

        try {
            serverProcess = processBuilder.start();
            System.out.printf("[%s] 服务 [%s] 启动成功\n", LocalDateTime.now(), serverName);
            return true;
        } catch (IOException e) {
            System.err.printf("[%s] 启动服务 [%s] 失败\n", LocalDateTime.now(), serverName, e.getMessage());
            return false;
        }
    }

    // stop server
    private static void stopService() {
        if (serverProcess != null && serverProcess.isAlive()) {
            System.out.printf("监测到服务 [%s] 正在运行, 尝试停止..\n.", serverName);
            serverProcess.destroy();

            try {
                serverProcess.waitFor(30, TimeUnit.SECONDS);
                System.out.printf("[%s] 服务 [%s] 已停止\n", LocalDateTime.now(), serverName);
            } catch (InterruptedException e) {
                System.err.printf("[%s] 停止服务 [%s] 失败: %s\n", LocalDateTime.now(), serverName, e.getMessage());
                serverProcess.destroyForcibly(); //
            }

        } else {
            System.out.printf("未找到在运行的服务 [%s]\n", serverName);
        }
    }

    private static boolean isRunning (int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
