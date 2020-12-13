import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SocketChatServer {

    private ServerSocket serverSocket;  // 서버의 소켓
    private Socket socket;              // 소켓
    private String messeage;            // 메시지 데이터
    private Integer port = 3003;        // 서버에서 열 Port

    // 사용자들의 정보를 저장하는 맵
    private Map<String, DataOutputStream> clientsMap = new HashMap<String, DataOutputStream>();

    public static void main(String[] args) throws IOException {
        SocketChatServer socketChatServer = new SocketChatServer();
        socketChatServer.setSocketServer();
    }

    public void setSocketServer() throws IOException {
        // 맵 동기화
        // clientMap을 동기화한 Map 리턴
        Collections.synchronizedMap(clientsMap);

        /** 서버 소켓 생성 **/
        serverSocket = new ServerSocket(port);

        // 반복해서 사용자들을 받는다
        while (true) {
            // 클라이언트가 들어오길 대기
            System.out.println("서버 소켓 대기중...");

            /** 클라이언트의 connect() -> 연결 요청이 오면 수락 **/
            socket = serverSocket.accept();

            // 각 클라이언트의 정보를 넣어 소켓 작업 쓰레드 생성
            SocketThread socketThread = new SocketThread(socket);
            socketThread.start();
        }
    }

    /** 소켓 작업 쓰레드 **/
    class SocketThread extends Thread {
        private DataInputStream dis;
        private DataOutputStream dos;
        private String nickName;

        /** 소켓 생성 **/
        public SocketThread(Socket socket) throws IOException {
            // 데이터를 주고 받을 통로 구축
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // 클라이언트가 생성되고 보내주는 첫 데이터 nickName
            nickName = dis.readUTF();

            // 클라이언트 정보 추가
            addClient(nickName, dos);
        }

        /** read() / write()를 반복해서 실행 **/
        public void run() {
            try {
                // DataInputStream이 있을 때까지
                while (dis != null) {
                    // 메시지 읽어들이기
                    messeage = dis.readUTF();
                    // 클라이언트로 메시지 전달
                    sendMessage(messeage);
                }
            } catch (IOException e) {
                // 클라이언트 소켓 통신 끊기면 에러 발생 -> 접속 종료 처리
                removeClient(nickName);
            } finally {
                // 모든 클라이언트(들)이 접속 종료 시
                if (clientsMap.size() == 0){
                    try{
                        System.out.println("*********** 접속중인 사용자 0 ***********");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 클라이언트 추가
    public void addClient(String nickName, DataOutputStream out) {
        // 서버 터미널에 클라이언트 접속 출력
        System.out.println("*********** " + socket.getInetAddress() + " " + nickName + "님 접속 ***********");
        // 클라이언트(들)에게 새로운 클라이언트 접속 메시지 전달
        sendMessage(nickName + "님이 접속하셨습니다\n");
        // 맵에 클라이언트 정보 추가(저장)
        clientsMap.put(nickName, out);
    }

    // 클라이언트 삭제
    public void removeClient(String nickName) {
        // 서버 터미널에 클라이언트 종료 출력
        System.out.println("*********** " + socket.getInetAddress() + " " + nickName + "님 종료 ***********");
        // 클라이언트(들)에게 접속 종료 메시지 전달
        sendMessage(nickName + "님이 나가셨습니다\n");
        //
        try{
            socket.close();
            System.out.println(nickName + " 소켓 close()");
        } catch (Exception e){
            System.out.println("에러");
        }
        // 맵에서 클라이언트 정부 삭제
        clientsMap.remove(nickName);

    }

    /** write() 메시지 내용 전파 **/
    public void sendMessage(String msg) {
        // Interator를 이용해 키(nickName) 가져오기
        // clientMap에 저장되어 있는 키들을 Set으로 반환 -> 키를 읽기 위해 Iterator 인터페이스 사용
        Iterator<String> iterator = clientsMap.keySet().iterator();
        String key = "";
        // 남아있는 키가 있을 때까지 즉, 클라이언트가 접속해있다면
        // hasNext() : 읽어올 요소가 남아있는지 확인하는 메소드
        while (iterator.hasNext()) {
            // 다음 키 반환 즉, 클라이언트 차례대로
            key = iterator.next();
            try {
                // 메시지 전달
                clientsMap.get(key).writeUTF(msg);
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
    }


}