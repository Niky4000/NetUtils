package ru.kiokle.simplehttpserver;

public class I2PClient {

//    public void connect(String destinationHost, int destinationPort, int proxyPort) throws IOException {
//        String readInputStream = null;
//        InetSocketAddress hiddenerProxyAddress = new InetSocketAddress("127.0.0.1", proxyPort);
//        Proxy hiddenProxy = new Proxy(Proxy.Type.HTTP, hiddenerProxyAddress);
//        try (Socket socket = new Socket(hiddenProxy)) {
//            InetSocketAddress sa = InetSocketAddress.createUnresolved(destinationHost, destinationPort);
//            socket.setSoTimeout(100000);
//            socket.connect(sa);
//            socket.setKeepAlive(true);
//            try (BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)) {
//                readInputStream = readInputStream(inputStream);
//            }
//        }
//        Logger.log(readInputStream != null ? readInputStream : "readInputStream is empty!");
//    }
//    public void connect2(String destinationHost, int destinationPort, int proxyPort) throws IOException, DataFormatException, I2PException {
//        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
//        I2PSocket socket = manager.connect(new Destination(destinationHost));
//        String readInputStream = readInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
//        Logger.log(readInputStream != null ? readInputStream : "readInputStream is empty!");
//    }


//    public void createI2PServer() {
//        I2PSocketManager manager = I2PSocketManagerFactory.createManager();
//        I2PServerSocket serverSocket = manager.getServerSocket();
//        I2PSession session = manager.getSession();
//        //Print the base64 string, the regular string would look like garbage.
//        Logger.log(session.getMyDestination().toBase64());
//    }
}
