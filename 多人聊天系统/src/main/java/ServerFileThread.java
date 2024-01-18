import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * 类名: ServerFileThread
 * 类功能: 文件服务线程
 */
public class ServerFileThread extends Thread {
    ServerSocket server = null;
    Socket socket = null;
    static List<User> user_list = new ArrayList<>();
    static Map<String, List<String>> groupMap = new HashMap<>();

    /**
     * 函数名: run
     * 函数功能: 启动文件服务线程，在 8090 端口监听客户端连接；
     *         建立连接后，创建用户，并创建文件传输线程
     */
    public void run() {
        try {
            server = new ServerSocket(8090);
            System.out.println("文件服务线程启动成功");

            while(true) {
                socket = server.accept();
                if (socket != null) {
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String json = inputStream.readUTF();
                    JSONObject data = JSONObject.fromObject(json);
                    String cat = data.getString("category");

                    if (Objects.equals(cat, "createUser")) {  // 新建用户报文
                        String username = data.getString("username");
                        System.out.println("文件服务线程收到新建用户报文，新用户为：" + username);

                        // 创建新用户
                        User user = new User();
                        user.setSocket(socket);
                        user.setUsername(username);
                        user_list.add(user);

                        // 创建文件传输线程
                        FileReadAndWrite fileReadAndWrite = new FileReadAndWrite(socket);
                        fileReadAndWrite.start();

                    } else if (Objects.equals(cat, "createGroup")) {  // 新建群聊报文
                        String groupName = data.getString("group");
                        System.out.println("文件服务线程收到新建群聊报文，群聊名称为：" + groupName);

                        // 创建群聊
                        JSONArray temp1 = data.getJSONArray("member");
                        List<String> temp2 = new ArrayList<>();
                        for (int i = 0; i < temp1.size(); i++) {
                            String str = temp1.getString(i);
                            temp2.add(str);
                        }
                        groupMap.put(groupName, temp2);

                    } else if (Objects.equals(cat, "addMember")) {  // 新增群聊成员报文
                        String groupName = data.getString("group");
                        System.out.println("文件服务线程收到新增群聊成员报文，群聊名称为：" + groupName);

                        JSONArray temp1 = data.getJSONArray("member");
                        List<String> temp2 = new ArrayList<>(groupMap.get(groupName));
                        for (int i = 0; i < temp1.size(); i++) {
                            String str = temp1.getString(i);
                            temp2.add(str);
                        }
                        groupMap.replace(groupName, temp2);

                    } else if (Objects.equals(cat, "deleteMember")) {  // 删除群聊成员 or 群聊成员退出报文
                        String groupName = data.getString("group");
                        System.out.println("文件服务线程收到删除群聊成员 or 群聊成员提出报文，群聊名为：" + groupName);

                        JSONArray temp1 = data.getJSONArray("member");
                        List<String> temp2 = new ArrayList<>(groupMap.get(groupName));
                        for (int i = 0; i < temp1.size(); i++) {
                            String str = temp1.getString(i);
                            temp2.remove(str);
                        }
                        groupMap.replace(groupName, temp2);

                    } else if (Objects.equals(cat, "clearGroup")) {  // 解散群聊报文
                        String groupName = data.getString("group");
                        System.out.println("文件服务线程收到解散群聊报文，群聊名为：" + groupName);

                        groupMap.remove(groupName);
                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * 类名: FileReadAndWrite
 * 类功能: 服务器文件传输线程
 */
class FileReadAndWrite extends Thread {
    private final Socket nowSocket;

    public FileReadAndWrite(Socket socket) {
        this.nowSocket = socket;
    }

    /**
     * 函数名: run
     * 函数功能: 接受客户端发来的文件并发送给其它客户端
     */
    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(nowSocket.getInputStream());

            while (true) {
                System.out.println("文件传输线程收到客户端发来的文件");

                String textName = inputStream.readUTF();    // 获取文件名
                String chatObject = inputStream.readUTF();  // 获取发送域
                String people = inputStream.readUTF();      // 获取发送者
                String category = inputStream.readUTF();    // 获取文件类型
                long textLength = inputStream.readLong();   // 获取文件长度
                System.out.println("文件名：" + textName + "，发送域：" + chatObject + "，发送者：" + people + "，类型：" + category + "，文件长度：" + textLength);

                DataOutputStream outputStream;
                if (chatObject.equals("聊天室")) {
                    System.out.println("进行文件聊天室内发送");
                    for (User u : ServerFileThread.user_list) {
                        if (u.getSocket() != nowSocket) {
                            outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                            outputStream.writeUTF(textName);
                            outputStream.flush();
                            outputStream.writeUTF(chatObject);
                            outputStream.flush();
                            outputStream.writeUTF(people);
                            outputStream.flush();
                            outputStream.writeUTF(category);
                            outputStream.flush();
                            outputStream.writeUTF("聊天室");
                            outputStream.flush();
                            outputStream.writeLong(textLength);
                            outputStream.flush();
                        }
                    }

                    int length;
                    long curLength = 0;
                    byte[] buff = new byte[1024];
                    while ((length = inputStream.read(buff)) > 0) {
                        curLength += length;
                        for (User u : ServerFileThread.user_list) {
                            if (u.getSocket() != nowSocket) {
                                outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                                outputStream.write(buff, 0, length);
                                outputStream.flush();
                            }
                        }
                        if (curLength >= textLength) {
                            break;
                        }
                    }

                } else if (ServerFileThread.groupMap.containsKey(chatObject)) {
                    System.out.println("进行文件组发");
                    for (User u : ServerFileThread.user_list) {
                        for (String str : ServerFileThread.groupMap.get(chatObject)) {
                            if (u.getUsername().equals(str) && u.getSocket() != nowSocket) {
                                outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                                outputStream.writeUTF(textName);
                                outputStream.flush();
                                outputStream.writeUTF(chatObject);
                                outputStream.flush();
                                outputStream.writeUTF(people);
                                outputStream.flush();
                                outputStream.writeUTF(category);
                                outputStream.flush();
                                outputStream.writeUTF("群聊");
                                outputStream.flush();
                                outputStream.writeLong(textLength);
                                outputStream.flush();
                            }
                        }
                    }

                    int length;
                    long curLength = 0;
                    byte[] buff = new byte[1024];
                    while ((length = inputStream.read(buff)) > 0) {
                        curLength += length;
                        for (User u : ServerFileThread.user_list) {
                            for (String str : ServerFileThread.groupMap.get(chatObject)) {
                                if (u.getUsername().equals(str) && u.getSocket() != nowSocket) {
                                    outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                                    outputStream.write(buff, 0, length);
                                    outputStream.flush();
                                }
                            }
                        }
                        if (curLength >= textLength) {
                            break;
                        }
                    }
                    
                } else {
                    System.out.println("进行文件私发");
                    for (User u : ServerFileThread.user_list) {
                        if (u.getUsername().equals(chatObject)) {
                            outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                            outputStream.writeUTF(textName);
                            outputStream.flush();
                            outputStream.writeUTF(chatObject);
                            outputStream.flush();
                            outputStream.writeUTF(people);
                            outputStream.flush();
                            outputStream.writeUTF(category);
                            outputStream.flush();
                            outputStream.writeUTF("私聊");
                            outputStream.flush();
                            outputStream.writeLong(textLength);
                            outputStream.flush();

                            int length;
                            long curLength = 0;
                            byte[] buff = new byte[1024];
                            while ((length = inputStream.read(buff)) > 0) {
                                curLength += length;
                                outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                                outputStream.write(buff, 0, length);
                                outputStream.flush();
                                if (curLength >= textLength) {
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            ServerFileThread.user_list.remove(nowSocket);  // 线程关闭，移除相应套接字
        }
    }

}