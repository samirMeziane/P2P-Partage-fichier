
public class Peer {

    String peerurl ;

    int peerport ;

    Peer(String url,int port){

        this.peerurl=url;
        this.peerport=port;

    }


    public void setPeerport(int peerport) {
        this.peerport = peerport;
    }

    public void setPeerurl(String peerurl) {
        this.peerurl = peerurl;
    }

    public int getPeerport() {
        return peerport;
    }

    public String getPeerurl() {
        return peerurl;
    }
}


