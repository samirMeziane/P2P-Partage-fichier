import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class Peer_Server implements Runnable {
	private volatile Selector selector = null;
	ServerSocketChannel server = null;

	private int port;
	ByteBuffer msgbuff =ByteBuffer.allocate(500);

	private List<Peer> peerslist;
	private HashMap<String,Long> fileslist;
	Charset charset = Charset.forName("UTF-8");
	volatile ByteBuffer bbf ;



	public Peer_Server(int port ) {
		this.port = port;
		this.peerslist=new ArrayList<>();
		bbf=ByteBuffer.allocate(100000);

		fileslist=new HashMap<>();

		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			InetSocketAddress adress = new InetSocketAddress(port);
			server.socket().bind(adress);
			selector = Selector.open();
			System.out.println("Serveur lancé");
			server.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
		}


	}





	public void run() {




		// Partie thread du serveur
		while (server.isOpen()) {
			try {
				selector.select();


				Iterator<SelectionKey> iteratorSK = selector.selectedKeys().iterator();

				while (iteratorSK.hasNext()) {

					SelectionKey sk = iteratorSK.next();
					iteratorSK.remove();

					if (sk.isAcceptable()) {

						registerClient(selector, server);}

					else if( sk.isReadable()) {

						SocketChannel channel = (SocketChannel) sk.channel();
						System.out.println("readable");
						processInputRequests(channel);

					} else if(sk.isWritable()){

						ByteBuffer bb;

						bb= (ByteBuffer) sk.attachment();
						SocketChannel schannel = (SocketChannel) sk.channel();
						while (schannel.write(bb)==0);

						bb.clear();

						Thread.sleep(400);
						schannel.register(selector,SelectionKey.OP_READ);
					}
					selector.selectedKeys().clear();
					//iteratorSK.remove();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


		}}





	private synchronized void processInputRequests(SocketChannel sc) throws IOException {



		bbf.clear();

		sc.read(bbf);


		bbf.flip();

		int ID = (int) bbf.get();



		switch (ID){

			case 1:

				int namefilelength = bbf.getInt();
				int bbfpos=bbf.position();
				while (bbf.position() != bbfpos+ namefilelength) {
					msgbuff.put(bbf.get());
				}
				msgbuff.flip();
				System.out.println(StandardCharsets.UTF_8.decode(msgbuff).toString());
				bbf.clear();
				msgbuff.clear();
				case 2:
				peerslist.add(new Peer(sc.getRemoteAddress().toString(),bbf.getInt()));

			case 3:
				bbf.clear();
				sendPeers(sc);

				break;
			case 5:
				bbf.clear();
				sendListeFiles(sc);

				break;
			case 7:

				sendFragment(sc);

				break;

			default:break;

		}




	}
	private synchronized void registerClient(Selector selector, ServerSocketChannel serverSocket) throws IOException {


		SocketChannel client = serverSocket.accept();
		if (client != null) {

			System.out.println("New client : " + client.getRemoteAddress());

			client.configureBlocking(false);
			client.register(selector,OP_READ);

		} else {

			System.err.println("Error with client registration..");
		}

	}






	public synchronized void sendFragment(SocketChannel sc) throws IOException{


		ByteBuffer bb= ByteBuffer.allocate(70000);

		int namefilelength = bbf.getInt();
		ByteBuffer filenamebuff = ByteBuffer.allocate(1024);

		int p = bbf.position()+namefilelength;

		while (bbf.position() != p) {
			filenamebuff.put(bb.get());
		}
		filenamebuff.flip();
		CharBuffer cb = charset.decode(filenamebuff);
		String fileNameChar = cb.toString();
		System.out.print(" sending......"+fileNameChar);
		long totalSize = bbf.getLong();
		long posRead = bbf.getLong();
		int fragmentSize = bbf.getInt();
		//pour pouvoir envoyer
		bbf.clear();

		// partie envoi
		bb.put((byte)8);
		bb.putInt(namefilelength);
		ByteBuffer fileNameByte = charset.encode(fileNameChar);
		bb.put(fileNameByte);
		bb.putLong(totalSize);
		bb.putLong(posRead);
		bb.putInt(fragmentSize);
		int position = (int)posRead;
		FileInputStream fis = new FileInputStream("src\\files\\"+fileNameChar);
		byte[] fileInput = new byte[fis.available()];

		fis.read(fileInput);
		fis.close();
		bb.put(fileInput, position, fragmentSize);
		System.err.println(bb.position());
		bb.flip();

		sc.keyFor(selector).attach(bb);
		sc.register(selector,OP_WRITE);
		selector.wakeup();
	}



	public synchronized void sendPeers(SocketChannel sc) throws ClosedChannelException {


		ByteBuffer bb = ByteBuffer.allocate(7000);

		bb.clear();
		int nbpeers = peerslist.size();
		bb.put((byte)4);
		bb.putInt(nbpeers);
		for (Peer peer : peerslist) {
			int port = peer.getPeerport();
			ByteBuffer host = charset.encode(peer.getPeerurl());
			bb.putInt(port);
			bb.putInt(host.limit());
			bb.put(host);
		}
		bb.flip();
		sc.keyFor(selector).attach(bb);
		sc.register(selector,OP_WRITE);
		selector.wakeup();

	}

	public synchronized void sendListeFiles(SocketChannel sch) throws IOException {


		ByteBuffer bb = ByteBuffer.allocate(10000);

		bb.clear();
		Map<String, Long> filesLists = new HashMap<>();

		File[] files = new File("src\\files\\").listFiles();
		for (File file : files) {
			if (file.isFile()) {
				filesLists.put(file.getName(), file.length());
			}
		}
		bb.put((byte)6);
		int nbFiles = filesLists.size();
		bb.putInt(nbFiles);


		filesLists.forEach((s, aLong) -> {

			ByteBuffer nameFile = charset.encode(s);
			int namefilelength = nameFile.limit();
			bb.putInt(namefilelength);
			bb.put(nameFile);

			bb.putLong(aLong);
		});
		bb.flip();

		sch.keyFor(selector).attach(bb);
		sch.register(selector,OP_WRITE);
		selector.wakeup();

	}



}
