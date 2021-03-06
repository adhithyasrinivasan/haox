package org.apache.haox.transport;

import org.apache.haox.event.AbstractEventHandler;
import org.apache.haox.event.Event;
import org.apache.haox.event.EventType;
import org.apache.haox.event.LongRunningEventHandler;
import org.apache.haox.transport.event.AddressEvent;
import org.apache.haox.transport.event.TransportEvent;
import org.apache.haox.transport.tcp.*;
import org.apache.haox.transport.udp.UdpAddressEvent;
import org.apache.haox.transport.udp.UdpEventType;
import org.apache.haox.transport.udp.UdpTransport;
import org.apache.haox.transport.udp.UdpTransportHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * A combined and mixed network facility handling UDP and TCP in both connect and accept sides
 */
public class Network extends LongRunningEventHandler {

    private Selector selector;
    private StreamingDecoder streamingDecoder;
    private UdpTransportHandler udpTransportHandler;
    private TcpTransportHandler tcpTransportHandler;

    class MyEventHandler extends AbstractEventHandler {
        @Override
        protected void doHandle(Event event) throws Exception {
            if (event.getEventType() == UdpEventType.ADDRESS_CONNECT) {
                doUdpConnect((AddressEvent) event);
            } else if (event.getEventType() ==  UdpEventType.ADDRESS_BIND) {
                doUdpBind((AddressEvent) event);
            } else if (event.getEventType() ==  TcpEventType.ADDRESS_CONNECT) {
                doTcpConnect((AddressEvent) event);
            } else if (event.getEventType() ==  TcpEventType.ADDRESS_BIND) {
                doTcpBind((AddressEvent) event);
            }
        }

        @Override
        public EventType[] getInterestedEvents() {
            return new EventType[]{
                    UdpEventType.ADDRESS_CONNECT,
                    UdpEventType.ADDRESS_BIND,
                    TcpEventType.ADDRESS_CONNECT,
                    TcpEventType.ADDRESS_BIND
            };
        }
    }

    public Network() {
        setEventHandler(new MyEventHandler());
    }

    @Override
    public void init() {
        super.init();

        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TCP transport only, for decoding tcp streaming into messages
     * @param streamingDecoder
     */
    public void setStreamingDecoder(StreamingDecoder streamingDecoder) {
        this.streamingDecoder = streamingDecoder;
    }

    /**
     * TCP only. Connect on the given server address. Can be called multiple times
     * for multiple servers
     * @param serverAddress
     * @param serverPort
     */
    public void tcpConnect(String serverAddress, short serverPort) {
        InetSocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        checkTcpTransportHandler();
        doTcpConnect(sa);
    }

    /**
     * UDP only. Connect on the given server address. Can be called multiple times
     * for multiple servers
     * @param serverAddress
     * @param serverPort
     */
    public void udpConnect(String serverAddress, short serverPort) {
        InetSocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        checkUdpTransportHandler();
        doUdpConnect(sa);
    }

    /**
     * TCP only. Listen and accept connections on the address. Can be called multiple
     * times for multiple server addresses.
     * @param serverAddress
     * @param serverPort
     */
    public void tcpListen(String serverAddress, short serverPort) {
        InetSocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        checkTcpTransportHandler();
        doTcpListen(sa);
    }

    /**
     * UDP only. Listen and accept connections on the address. Can be called multiple
     * times for multiple server addresses.
     * @param serverAddress
     * @param serverPort
     */
    public void udpListen(String serverAddress, short serverPort) {
        InetSocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        checkUdpTransportHandler();
        doUdpListen(sa);
    }

    @Override
    protected void loopOnce() {
        try {
            selectOnce();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void selectOnce() throws IOException {
        if (selector.isOpen() && selector.select(2) > 0 && selector.isOpen()) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                dealKey(selectionKey);
                iterator.remove();
            }
            selectionKeys.clear();
        }
    }

    private void checkTcpTransportHandler() {
        if (tcpTransportHandler == null) {
            if (streamingDecoder == null) {
                throw new IllegalArgumentException("No streaming decoder set yet");
            }
            tcpTransportHandler = new TcpTransportHandler(streamingDecoder);
            getDispatcher().register(tcpTransportHandler);
        }
    }

    private void checkUdpTransportHandler() {
        if (udpTransportHandler == null) {
            udpTransportHandler = new UdpTransportHandler();
            getDispatcher().register(udpTransportHandler);
        }
    }

    private void dealKey(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isConnectable()) {
            doTcpConnect(selectionKey);
        } else if (selectionKey.isAcceptable()) {
            doTcpAccept(selectionKey);
        } else {
            helpHandleSelectionKey(selectionKey);
        }
    }

    private void helpHandleSelectionKey(SelectionKey selectionKey) throws IOException {
        SelectableChannel channel = selectionKey.channel();
        if (channel instanceof DatagramChannel) {
            udpTransportHandler.helpHandleSelectionKey(selectionKey);
        } else {
            tcpTransportHandler.helpHandleSelectionKey(selectionKey);
        }
    }

    private void doUdpConnect(InetSocketAddress sa) {
        AddressEvent event = UdpAddressEvent.createAddressConnectEvent(sa);
        dispatch(event);
    }

    private void doUdpConnect(AddressEvent event) throws IOException {
        InetSocketAddress address = event.getAddress();
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);

        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        UdpTransport transport = new UdpTransport(channel, address);
        onNewTransport(transport);
    }

    protected void doUdpListen(InetSocketAddress socketAddress) {
        AddressEvent event = UdpAddressEvent.createAddressBindEvent(socketAddress);
        dispatch(event);
    }

    private void doUdpBind(AddressEvent event) throws IOException {
        DatagramChannel serverChannel = DatagramChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(event.getAddress());
        serverChannel.register(selector, SelectionKey.OP_READ);
    }

    protected void doTcpConnect(InetSocketAddress sa) {
        AddressEvent event = TcpAddressEvent.createAddressConnectEvent(sa);
        dispatch(event);
    }

    private void doTcpConnect(AddressEvent event) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(event.getAddress());
        channel.register(selector,
                SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void doTcpConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }

        Transport transport = new TcpTransport(channel, tcpTransportHandler.getStreamingDecoder());
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, transport);
        onNewTransport(transport);
    }

    protected void doTcpListen(InetSocketAddress socketAddress) {
        AddressEvent event = TcpAddressEvent.createAddressBindEvent(socketAddress);
        dispatch(event);
    }

    void doTcpAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel;
        while ((channel = server.accept()) != null) {
            // Quick fix: avoid exception during exiting
            if (! selector.isOpen()) {
                channel.close();
                break;
            };

            channel.configureBlocking(false);
            channel.socket().setTcpNoDelay(true);
            channel.socket().setKeepAlive(true);

            Transport transport = new TcpTransport(channel,
                    tcpTransportHandler.getStreamingDecoder());
            channel.register(selector,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE, transport);
            onNewTransport(transport);
        }
    }

    protected void doTcpBind(AddressEvent event) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(event.getAddress());
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, serverSocketChannel);
    }

    private void onNewTransport(Transport transport) {
        transport.setDispatcher(getDispatcher());
        dispatch(TransportEvent.createNewTransportEvent(transport));
    }
}
