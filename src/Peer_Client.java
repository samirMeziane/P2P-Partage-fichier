

import javax.swing.*;
import java.io.*;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;

import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_WRITE;





public class Peer_Client  {


	private SocketChannel sc;

	private int port;
	private String ip;
	volatile private ByteBuffer wbb;
	private Selector selector;
	ByteBuffer file=ByteBuffer.allocate(1024);


	Charset charset = Charset.forName("UTF-8");
	private long filecurrentposition=0;
	HashMap<String,Long> fileslist;
	List<Peer> peerslist;
	int listenport;

	ByteBuffer msgbuff =ByteBuffer.allocate(500);

	private List<ByteBuffer> byteBufferList;
	volatile 	private ByteBuffer rbb;

	private int readcount =0;

	public Peer_Client(int port) throws IOException {

		this.port=port;
		this.wbb = ByteBuffer.allocate(100000);
		this.rbb = ByteBuffer.allocate(100000);


		byteBufferList=new ArrayList<>();
		peerslist = new ArrayList<>();
		fileslist = new HashMap<>();
		selector = Selector.open();
	}






	public void ConnectToPeer(String ip, int port) throws IOException {

		this.sc = SocketChannel.open();
		sc.connect(new InetSocketAddress(ip, port));
		sc.configureBlocking(false);

		sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
		sc.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		System.out.println("Client connecté");
		sc.register(selector,SelectionKey.OP_READ);
		processServerResponse(selector);


		//t.start();
	}


	public void processServerResponse(Selector s) throws IOException {

		byte ID;



		while (sc.isOpen()){

			s.select();

			Iterator<SelectionKey> i = s.selectedKeys().iterator();


			while(i.hasNext()) {
				try {
					SelectionKey sk = i.next();
					i.remove();

					if (sk.isReadable()) {
						SocketChannel schannel = (SocketChannel) sk.channel();


							while ((readcount=schannel.read(rbb))==0);

							//à vérifier
							while (schannel.isConnectionPending()){

								schannel.register(selector,SelectionKey.OP_READ);
								selector.wakeup();
							}

						    rbb.flip();

						    ID = rbb.get();
							inputsFromServer(ID);
							rbb.clear();


					}
					if (sk.isWritable()){

						ByteBuffer bb;

						bb= (ByteBuffer) sk.attachment();
						SocketChannel schannel = (SocketChannel) sk.channel();
						while (schannel.write(bb)==0);

						bb.clear();

						//important
						Thread.sleep(1500);
						schannel.register(selector,SelectionKey.OP_READ);


					}
					selector.selectedKeys().clear();
				}catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}}


	}





	private void inputsFromServer(byte id) {
		try {
			System.out.println(id+"id");

			switch (id){


				case 1:
					int namefilelength = rbb.getInt();
					int bbfpos=rbb.position();
					while (rbb.position() != bbfpos+ namefilelength) {
						msgbuff.put(rbb.get());
					}
					msgbuff.flip();
					System.out.println(StandardCharsets.UTF_8.decode(msgbuff).toString());
					rbb.clear();
					msgbuff.clear();
					break;

				case 3:

					sendPeers(sc);
					break;
				case 4:

					getListOfPeers();
					break;
				case 5:

					sendFilesToPeer(sc);
					break;
				case 6:
					getListOfFiles();
					break;

				case 7:
					sendFragToPeer(sc);
					break;
				case 8:
					downloadFile();
					break;

				default:
					rbb.clear();
					break;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}











	public    void getListOfFiles() throws ClosedChannelException {



		CharBuffer charBuffer;


		int nbF = rbb.getInt();



		for (int i = 1; i <= nbF; i++){
			int namelength = rbb.getInt();
			ByteBuffer filename = ByteBuffer.allocate(10000);
			int p = rbb.position()+namelength;
			while (rbb.position() != p) {

				filename.put(rbb.get());
			}

			long filesize = rbb.getLong();
			filename.flip();

			charBuffer= charset.decode(filename);
			fileslist.put(charBuffer.toString(), filesize);

			filename.clear();


		}

		fileslist.forEach((s, aLong) -> System.out.println(s+"-----"+aLong));

		this.setFileslist(fileslist);
		rbb.clear();

	}




	public    void  getListOfPeers() {




		int nbpeers = rbb.getInt();
		peerslist.clear();
		for (int i = 1; i <= nbpeers; i++){
			int port = rbb.getInt();
			int peerlgth = rbb.getInt();

			ByteBuffer bufpeer = ByteBuffer.allocate(1024);
			int p = rbb.position()+peerlgth;
			while (rbb.position() != p){
				bufpeer.put(rbb.get());
			}
			bufpeer.flip();


			CharBuffer cb = charset.decode(bufpeer);
			peerslist.add(new Peer(cb.toString(),port));
		}




		peerslist.forEach(peer -> System.out.println(peer.getPeerurl()+":port"+peer.getPeerport()) );

		this.setPeerslist(peerslist);
		rbb.clear();

	}





	public void setPeerslist(List<Peer> peerslist) {
		this.peerslist = peerslist;
	}

	public void setFileslist(HashMap<String, Long> fileslist) {
		this.fileslist = fileslist;
	}



	public HashMap<String, Long> getFileslist() {
		return fileslist;
	}

	public SocketChannel getSc() {
		return sc;
	}

	public List<Peer> getPeerslist() {
		return peerslist;
	}



	public void declarePort() {
		ByteBuffer bb =ByteBuffer.allocate(50);
		bb.clear();
		bb.put((byte) 2);
		bb.putInt(port);
		setForWriteOp(bb);

	}


	public void requestListOfPeers() {
		ByteBuffer bb =ByteBuffer.allocate(50);
		bb.clear();
		bb.put((byte) 3);

		setForWriteOp(bb);

	}







	public  void requestListOfFiles( ){

		ByteBuffer bb =ByteBuffer.allocate(50);
		bb.clear();
		bb.put((byte) 5);

		setForWriteOp(bb);
	}



	void sendMessage(String msg){
		ByteBuffer bb =ByteBuffer.allocate(1000);
		bb.put((byte) 1);
		int length =charset.encode(msg).array().length;
		bb.putInt(length);
		bb.put(charset.encode(msg).array());
		setForWriteOp(bb);


	}

	long fragsize=0;


	public  void  downloadFile() throws IOException {



		long positiondemende;
		int nbbytesdemende;
		ByteBuffer filenamebuff = ByteBuffer.allocate(1024);
		long filesize;
		String filename;



		int namefilelength = rbb.getInt();
		int rbbpos=rbb.position();
		while (rbb.position() != rbbpos+ namefilelength) {
			filenamebuff.put(rbb.get());
		}
		filenamebuff.flip();
		filename =  StandardCharsets.UTF_8.decode(filenamebuff).toString();


		filesize= rbb.getLong();

		positiondemende=rbb.getLong();

		nbbytesdemende=rbb.getInt();



		System.out.println("position demandé "+positiondemende+"nombre de bytes demandé"+nbbytesdemende);

		if (file.position()==0){

			file=ByteBuffer.allocate((int) filesize);
			file.clear();
		}


		System.err.println("read COUNT DAND DOWNLOAD"+readcount);




		System.err.println("rbb positon apres le if read"+readcount);

		while (filecurrentposition==file.position()){

			file.put(rbb.get());

		}

		while (rbb.hasRemaining()) {
			file.put(rbb.get());
		}

		filecurrentposition=file.position();

		if (filecurrentposition!=filesize){
			rbb.clear();
			requestFragment(filename,filesize);
		}else {
			file.flip();

			FileOutputStream fos = new FileOutputStream("src\\files\\"+filename);
			fos.write(file.array());
			fos.close();
			filecurrentposition=0;
			rbb.clear();


		}



	}






	public void requestFragment(String filename, long  filesize) throws IOException {


		ByteBuffer bb =ByteBuffer.allocate(1000);



		System.out.println("dans reuest fragment");



		ByteBuffer filenbuff = charset.encode(filename);


		if(filesize-filecurrentposition >= 65536){
			fragsize = 65536;

		}else{
			fragsize=filesize-filecurrentposition;
		}

		int namelgth = filenbuff.limit();


		bb.put((byte) 7);
		bb.putInt(namelgth);
		bb.put(filenbuff);
		bb.putLong(filesize);
		bb.putLong(filecurrentposition);
		bb.putInt((int) fragsize);

		setForWriteOp(bb);


	}

	 void setForWriteOp(ByteBuffer bb) {
		try {

			bb.flip();

			sc.register(selector, OP_WRITE);
			sc.keyFor(selector).attach(bb);
			selector.wakeup();

		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public  void sendFragToPeer(SocketChannel sc) throws IOException{


		ByteBuffer bb =ByteBuffer.allocate(100000);


		int namefilelength = rbb.getInt();
		ByteBuffer filenamebuff = ByteBuffer.allocate(2000);

		int p = rbb.position()+namefilelength;

		while (rbb.position() != p) {
			filenamebuff.put(rbb.get());
		}
		filenamebuff.flip();
		CharBuffer cb = charset.decode(filenamebuff);
		String filename = cb.toString();

		System.out.print(" sending............................ to fragment to peer"+filename);
		long totalSize = rbb.getLong();
		long posRead = rbb.getLong();
		int fragmentSize = rbb.getInt();
		//pour pouvoir envoyer
		rbb.clear();

		bb.clear();
		// partie envoi
		bb.put((byte)8);
		bb.putInt(namefilelength);
		ByteBuffer decodenamebuff = charset.encode(filename);
		bb.put(decodenamebuff);
		bb.putLong(totalSize);
		bb.putLong(posRead);
		bb.putInt(fragmentSize);
		int position = (int)posRead;
		FileInputStream is = new FileInputStream("src\\files\\"+filename);
		byte[] fileInput = new byte[is.available()];

		is.read(fileInput);
		is.close();
		bb.put(fileInput, position, fragmentSize);
		System.err.println(bb.position());

		setForWriteOp(bb);

	}



	public  void sendPeers(SocketChannel sc) throws ClosedChannelException {

		ByteBuffer bb =ByteBuffer.allocate(1000);
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
		setForWriteOp(bb);



	}

	public  void sendFilesToPeer(SocketChannel sch) throws IOException {



		ByteBuffer bb =ByteBuffer.allocate(1000);
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


		setForWriteOp(bb);

	}




	public void close() {
		try {
			sc.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws  IOException {
		JTextField url = new JTextField();
		JTextField port = new JTextField();
		Object[] message = {
				"Peer url:", url,
				"port:", port
		};

			int serverport=1002;


			Peer_Server peer_server =new Peer_Server(serverport);
			Thread thread =new Thread(peer_server);
			thread.start();

			Peer_Client client=new Peer_Client(serverport);


			MenuPrincipal menuPrincipal =new MenuPrincipal(client) ;

		int option = JOptionPane.showConfirmDialog(null, message, "Add peer", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {

			client.ConnectToPeer(url.getText(),Integer.parseInt(port.getText()));
		} else {

			System.out.println("ajout du pair annulé");
		}


	}


}
