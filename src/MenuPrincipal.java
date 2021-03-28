import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class MenuPrincipal extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList list1;
    private JList list2;
    private JList list3;
    private JTextField textField1;
    private JButton okButton2;
    private JButton okButton1;
    private JButton okButton;
    private JButton button1;
    private JTextArea textArea1;
    private JButton addButton;
    private JTextArea logsTextArea;
    private int command =0;
    private long filesize=0;
    private int fileid;
    String filename;
    Peer_Client peer_client;

    public MenuPrincipal(Peer_Client peer_client) {


        this.peer_client=peer_client;


        setContentPane(contentPane);
       // this.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        getRootPane().setDefaultButton(buttonOK);
        this.pack();
        this.setVisible(true);


        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        okButton2.addActionListener(e -> {
            peer_client.declarePort();
        });


        okButton1.addActionListener(e -> {

            peer_client.requestListOfPeers();


            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            putInListOfPeers(peer_client.getPeerslist());
            textArea1.append("list of files ok");
        });
        okButton.addActionListener(e -> {

            peer_client.requestListOfFiles();


            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            putInListOfFiles(peer_client.getFileslist());
        });


        button1.addActionListener(e -> peer_client.sendMessage(textField1.getText()));


        //list des paire
        list3.addMouseListener(new MouseAdapter() {

            String urlandport;
            String url;
            int port;
            int index;

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (e.getClickCount()==2){

                    urlandport= (String)list3.getSelectedValue();
                    index=urlandport.indexOf("/");
                    url =urlandport.substring(0,index);
                    port=Integer.parseInt(urlandport.substring(index+1));

                    try {
                        //connexion à un paire
                        peer_client.ConnectToPeer(url,port);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        });

        //list des fichier du pair
        list1.addMouseListener(new MouseAdapter() {
            String filename;
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (e.getClickCount()==2){

                    filename=(String) list1.getSelectedValue();
                    //downloadfile
                    try {

                        peer_client.requestFragment(filename,peer_client.fileslist.get(filename));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        });

        //button envoi de message
        button1.addActionListener(e -> {
            peer_client.sendMessage(textField1.getText());

        });



    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }



    public  void putInListOfPeers(List<Peer> peerList){
        Vector<String> filesvector;


        filesvector= new Vector<>();
        peerList.forEach((peer) ->filesvector.addElement(peer.getPeerurl()+"/"+peer.getPeerport()));


        list3.setListData(filesvector);
    }


    public  void putInListOfFiles(HashMap<String,Long> listofpeers){
        Vector<String> filesvector;



        filesvector= new Vector<>();
        listofpeers.forEach((s, aLong) -> filesvector.addElement(s));

        list1.setListData(filesvector);


    }
    public int getCommand() {
        return command;
    }

    public void setCommand (int c){


        this.command=c;
    }



    public long getFilesize() {
        return filesize;
    }

    public String getFilename() {
        return filename;
    }




}
