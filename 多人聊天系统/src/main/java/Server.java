import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class Server extends JFrame{

    //用户列表，用于存放连接上的用户信息
    ArrayList<User> user_list = new ArrayList<>();

    //用户名列表，用于显示已连接上的用户
    ArrayList<String> username_list = new ArrayList<>();

    Map<String, List<String>> groupMap = new HashMap<>();

    //消息显示区域
    JTextArea show_area = new JTextArea();
    //用户名显示区域
    JTextArea show_user = new JTextArea(10, 10);

    //socket的数据输出流
    DataOutputStream outputStream = null;
    //socket的数据输入流
    DataInputStream inputStream = null;

    //从主函数里面开启服务端
    public static void main(String[] args) {
        new Server();
    }

    //构造函数
    public Server() {
        // 创建文件服务线程
        ServerFileThread serverFileThread = new ServerFileThread();
        serverFileThread.start();

        // 设置布局
        setLayout(new BorderLayout());
        JScrollPane panel = new JScrollPane(show_area,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.setBorder(new TitledBorder("信息显示区"));
        add(panel,BorderLayout.CENTER);
        show_area.setEditable(false);

        final JPanel panel_east = new JPanel();
        panel_east.setLayout(new BorderLayout());
        panel_east.setBorder(new TitledBorder("在线用户"));
        panel_east.add(new JScrollPane(show_user), BorderLayout.CENTER);
        show_user.setEditable(false);
        add(panel_east, BorderLayout.EAST);

        final JPanel panel_south = new JPanel();
        JLabel label = new JLabel("输入要踢下线用户的ID");
        JTextField out_area = new JTextField(40);
        JButton out_btn = new JButton("踢下线");
        panel_south.add(label);
        panel_south.add(out_area);
        panel_south.add(out_btn);
        add(panel_south, BorderLayout.SOUTH);

        // 点击踢下线按钮触发事件
        out_btn.addActionListener(e -> {
            try {
                //用于存储踢下线用户的名字
                String out_username;
                //从输入框中获取踢下线用户名
                out_username = out_area.getText().trim();
                //用于判断盖用户是否被踢下线
                boolean is_out=false;
                //遍历用户列表依次判断
                for (int i = 0; i < user_list.size(); i++){
                    //比较用户名，相同则踢下线
                    if(user_list.get(i).getUsername().equals(out_username)){
                        //获取被踢下线用户对象
                        User out_user = user_list.get(i);
                        //将被踢用户移出用户列表
                        user_list.remove(i);
                        //将被踢用户移出用户名列表
                        username_list.remove(out_user.getUsername());
                        //使用json封装将要传递的数据
                        JSONObject data = new JSONObject();
                        //封装全体用户名，广播至所有用户
                        data.put("user_list", username_list);
                        //广播的信息内容
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        String time = sdf.format(new Date());
                        data.put("msg", out_user.getUsername() +"在"+time+"被管理员踢出\n");
                        //服务端消息显示区显示相应信息
                        show_area.append(out_user.getUsername() + "被你踢出\n");
                        data.put("category","user");
                        data.put("private","groupChat");
                        //依次遍历用户列表
                        for (User value : user_list) {
                            try {
                                //获取每个用户列表的socket连接
                                outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                                //传递信息
                                outputStream.writeUTF(data.toString());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                        //获取被踢用户列表的socket连接
                        outputStream = new DataOutputStream(out_user.getSocket().getOutputStream());
                        //传递信息
                        outputStream.writeUTF(data.toString());

                        //刷新在线人数
                        show_user.setText("人数有 " + username_list.size() + " 人\n");
                        //刷新在线用户
                        for (String s : username_list) {
                            show_user.append(s + "\n");
                        }
                        //判断踢出成功
                        is_out=true;
                        break;
                    }

                }
                //根据是否踢出成功弹出相应提示
                if(is_out){
                    JOptionPane.showMessageDialog(null,"踢下线成功","提示",
                            JOptionPane.WARNING_MESSAGE);
                }
                if(!is_out){
                    JOptionPane.showMessageDialog(null,"不存在用户","提示",
                            JOptionPane.WARNING_MESSAGE);
                }
                //重置输入框
                out_area.setText("");
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        });

        //设置该窗口名
        setTitle("服务器 ");
        //引入图片
        BufferedImage img;
        try {
            //根据图片名引入图片
            img = ImageIO.read(Server.class.getResource("/Server.png"));
            //设置其为该窗体logo
            setIconImage(img);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        //设置窗体大小
        setSize(700, 500);
        //设置窗体位置可移动
        setLocationRelativeTo(null);
        //设置窗体关闭方式
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //设置窗体可见
        setVisible(true);

        // socket连接相关代码
        try {
            // Creates a server socket, bound to the specified port 11111.
            ServerSocket serverSocket = new ServerSocket(11111);
            show_area.append("服务器启动时间 " + new Date() + "\n");

            // 持续接收连接
            while (true) {
                // Listens for a connection to be made to serverSocket and accepts it.
                // The method blocks until a connection is made.
                Socket socket = serverSocket.accept();

                if (socket != null) {
                    inputStream = new DataInputStream(socket.getInputStream());
                    String json = inputStream.readUTF();
                    JSONObject data = JSONObject.fromObject(json);
                    String username = data.getString("username");
                    String time = data.getString("time");

                    show_area.append("用户 " + username + " 在 " + time + " 登陆系统"+"\n");

                    // 在服务器中创建用户
                    User user = new User();
                    user.setSocket(socket);
                    user.setUsername(username);
                    user_list.add(user);
                    username_list.add(username);

                    // 刷新 show_user
                    show_user.setText("人数有 " + username_list.size() + " 人\n");
                    for (String s : username_list) {
                        show_user.append(s + "\n");
                    }

                    // 将该用户登陆上线的消息广播给所有在线用户
                    JSONObject online = new JSONObject();
                    online.put("category", "update");
                    online.put("private", "聊天室");
                    online.put("msg", time + " "  + "用户“" + username + "”上线了");
                    online.put("user_list", username_list);
                    for (User u : user_list) {
                        outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                        outputStream.writeUTF(online.toString());
                    }

                    // 创建 ServerThread 线程，持续接收该 socket 信息
                    new Thread(new ServerThread(socket)).start();

                }

            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 类名: ServerThread
     * 类功能: 接收客户端的 socket 发来的报文
     */
    class ServerThread implements Runnable {
        //存放全局变量socket
        private final Socket socket;

        //构造函数，初始化socket
        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                while (true) {
                    String json = inputStream.readUTF();
                    JSONObject data = JSONObject.fromObject(json);
                    String category = data.getString("category");
                    String username = data.getString("username");

                    if (Objects.equals(category, "createGroup")) {  // 建立群聊报文/添加、删除成员报文/解散群聊报文/退出群聊报文
                        String pri2 = data.getString("private");
                        String flag = data.getString("flag");
                        String msg;

                        if (Objects.equals(flag, "create")) {  // 建立群聊报文
                            System.out.println("收到建立群聊报文");

                            // 获取群聊成员：temp2
                            JSONArray temp1 = data.getJSONArray("member");
                            List<String> temp2 = new ArrayList<>();
                            for (int i = 0; i < temp1.size(); i++) {
                                String str = temp1.getString(i);
                                temp2.add(str);
                            }
                            groupMap.put(pri2, temp2);

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            String time = sdf.format(new Date());
                            msg = time + "群主“" + username + "”建立了群聊，" + "群内成员有：" + temp2;
                            show_area.append(msg);

                            // 发给群聊内除群主外所有用户
                            for (String str : temp2) {
                                if (Objects.equals(str, username)) {
                                    continue;
                                }
                                for (int i = 0; i < user_list.size(); i++) {
                                    if (user_list.get(i).getUsername().equals(str)) {
                                        createGroup(i, pri2, msg, temp1, username);
                                    }
                                }
                            }

                        } else if (Objects.equals(flag, "add")) {  // 添加成员报文
                            System.out.println("收到添加成员报文");

                            // 群聊成员：groupMap（老成员）+ temp2（新成员）
                            JSONArray temp1 = data.getJSONArray("member");
                            List<String> temp2 = new ArrayList<>();
                            for (int i = 0; i < temp1.size(); i++) {
                                String str = temp1.getString(i);
                                groupMap.get(pri2).add(str);
                                temp2.add(str);
                            }
                            JSONArray newMember = new JSONArray();
                            newMember.addAll(groupMap.get(pri2));

                            // 发给所有成员（但发给新成员和老成员的消息不同）
                            for (String str : groupMap.get(pri2)) {
                                if (temp2.contains(str)) {  // 新成员
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    String time = sdf.format(new Date());
                                    msg = time + "群主“" + username + "”建立了群聊，" + "群内成员有：" + pri2;
                                    show_area.append(msg);

                                    for (int i = 0; i < user_list.size(); i++) {
                                        if (user_list.get(i).getUsername().equals(str)) {
                                            createGroup(i, pri2, msg, newMember, username);
                                        }
                                    }

                                } else { // 老成员
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    String time = sdf.format(new Date());
                                    msg = time + temp2 + "加入了群聊，Welcome!";
                                    show_area.append(msg);

                                    for (int i = 0; i < user_list.size(); i++) {
                                        if (user_list.get(i).getUsername().equals(str)) {
                                            updateGroup(i, pri2, msg, newMember);
                                        }
                                    }
                                }

                            }

                        } else if (Objects.equals(flag, "delete")) {  // 删除群聊成员报文
                            System.out.println("收到删除群聊成员报文");

                            // 群聊成员 = groupMap（老成员）- temp2（被踢成员）
                            List<String> temp = new ArrayList<>(groupMap.get(pri2));
                            JSONArray temp1 = data.getJSONArray("member");
                            List<String> temp2 = new ArrayList<>();
                            for (int i = 0; i < temp1.size(); i++) {
                                String str = temp1.getString(i);
                                groupMap.get(pri2).remove(str);
                                temp2.add(str);
                            }
                            JSONArray newMember = new JSONArray();
                            newMember.addAll(groupMap.get(pri2));

                            // 发给所有成员（但发给被踢成员和其它人的消息不同）
                            for (String str : temp) {
                                if (temp2.contains(str)) {  // 被踢成员
                                    for (int i = 0; i < user_list.size(); i++) {
                                        if (user_list.get(i).getUsername().equals(str)) {
                                            deleteGroup(i, pri2);
                                        }
                                    }

                                } else {  // 其它人
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                    String time = sdf.format(new Date());
                                    msg = (time + temp2 + "被群主踢出了群聊");
                                    show_area.append(msg);

                                    for (int i = 0; i < user_list.size(); i++) {
                                        if (user_list.get(i).getUsername().equals(str)) {
                                            updateGroup(i, pri2, msg, newMember);
                                        }
                                    }
                                }
                            }

                        } else if (Objects.equals(flag, "clear")) {  // 解散群聊报文
                            System.out.println("收到解散群聊报文");

                            List<String> temp = new ArrayList<>(groupMap.get(pri2));
                            groupMap.remove(pri2);

                            // 发给群聊内所有成员
                            for (String str : temp) {
                                for (int i = 0; i < user_list.size(); i++) {
                                    if (user_list.get(i).getUsername().equals(str)) {
                                        clearGroup(i, pri2);
                                    }
                                }
                            }

                        } else if (Objects.equals(flag, "bye")) {  // 退出群聊报文
                            System.out.println("收到退出群聊报文");

                            groupMap.get(pri2).remove(username);
                            List<String> temp = new ArrayList<>(groupMap.get(pri2));
                            JSONArray newMember = new JSONArray();
                            newMember.addAll(groupMap.get(pri2));

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            String time = sdf.format(new Date());
                            msg = (time + " 用户“" + username + "”退出了群聊");
                            show_area.append(msg);

                            // 发给群聊内所有成员
                            for (String str : temp) {
                                for (int i = 0; i < user_list.size(); i++) {
                                    if (user_list.get(i).getUsername().equals(str)) {
                                        updateGroup(i, pri2, msg, newMember);
                                    }
                                }
                            }
                        }

                    } else if (Objects.equals(category, "user")) {  // 私聊报文
                        String msg = data.getString("time") + " " + username + "\n" + data.getString("msg");
                        String target = data.getString("private");
                        show_area.append(msg);

                        for (int i = 0; i < user_list.size(); i++) {
                            if (user_list.get(i).getUsername().equals(target)) {
                                send_msg(i, username, msg);
                            }
                        }

                    } else if (Objects.equals(category, "group")) {  // 群聊报文
                        String msg = data.getString("time") + " " + username + "\n" + data.getString("msg");
                        String pri2 = data.getString("private");
                        show_area.append(msg);

                        // 发给群聊内除发送者外所有用户
                        for (String str : groupMap.get(pri2)) {
                            if (Objects.equals(str, username)) {
                                continue;
                            }
                            for (int i = 0; i < user_list.size(); i++) {
                                if (user_list.get(i).getUsername().equals(str)) {
                                    send_msg(i, pri2, msg);
                                }
                            }
                        }

                    } else if (Objects.equals(category, "all")) {  // 聊天室报文
                        String msg = data.getString("time") + " " + username + "\n" + data.getString("msg");
                        String pri2 = "聊天室";
                        show_area.append(msg);

                        // 发给聊天室内除发送者外所有用户
                        if(msg.contains("下线")){
                            for (int i = 0; i < user_list.size(); i++){
                                //比较用户名，相同则踢下线
                                if(user_list.get(i).getUsername().equals(username)){
                                    //获取被踢下线用户对象
                                    User out_user = user_list.get(i);
                                    //将被踢用户移出用户列表
                                    user_list.remove(i);
                                    //将被踢用户移出用户名列表
                                    username_list.remove(out_user.getUsername());
                                    //使用json封装将要传递的数据
                                    JSONObject data1 = new JSONObject();
                                    //封装全体用户名，广播至所有用户
                                    data1.put("user_list", username_list);
                                    //广播的信息内容
                                    data1.put("msg", data.getString("time") + " 用户“" + out_user.getUsername() + "”下线了\n");
                                    //服务端消息显示区显示相应信息
                                    show_area.append(out_user.getUsername() + "下线\n");
                                    data1.put("time",data.getString("time"));
                                    data1.put("category","user");
                                    data1.put("private","groupChat");
                                    //依次遍历用户列表
                                    for (User value : user_list) {
                                        try {
                                            //获取每个用户列表的socket连接
                                            outputStream = new DataOutputStream(value.getSocket().getOutputStream());
                                            //传递信息
                                            outputStream.writeUTF(data1.toString());
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                    }

                                    //刷新在线人数
                                    show_user.setText("人数有 " + username_list.size() + " 人\n");
                                    //刷新在线用户
                                    for (String s : username_list) {
                                        show_user.append(s + "\n");
                                    }
                                    break;
                                }

                            }
                        }
                        else {
                            for (int i = 0; i < user_list.size(); i++) {
                                if (Objects.equals(user_list.get(i).getUsername(), username)) {
                                    continue;
                                }
                                send_msg(i, pri2, msg);
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 函数名称: send_msg
         * 函数功能: 发送正常消息报文
         * @param i: 发送对象为 user_list[i]
         * @param pri: 消息域
         * @param msg: 消息正文
         */
        private void send_msg(int i, String pri, String msg) {
            JSONObject data = createJSON("user", pri, msg, null, "");
            sendMessage(i, data);
        }

        /**
         * 函数名称: createGroup
         * 函数功能: 发送建立群聊报文
         * @param i: 发送对象为 user_list[i]
         * @param pri: 消息域（群聊名称）
         * @param msg: 消息正文
         * @param groupMember: 群聊内所有成员
         */
        private void createGroup(int i, String pri, String msg, JSONArray groupMember, String master) {
            JSONObject data = createJSON("createGroup", pri, msg, groupMember, master);
            sendMessage(i, data);
        }

        private void updateGroup(int i, String pri, String msg, JSONArray newMember) {
            JSONObject data = createJSON("updateGroup", pri, msg, newMember, "");
            sendMessage(i, data);
        }

        private void deleteGroup(int i, String pri) {
            JSONObject data = createJSON("deleteGroup", pri, "", null, "");
            sendMessage(i, data);
        }

        private void clearGroup(int i, String pri) {
            JSONObject data = createJSON("clearGroup", pri, "", null, "");
            sendMessage(i, data);
        }

        /**
         * 函数名称: JSONObject
         * 函数功能: 封装 JSON 对象
         * @param category: 消息类型
         * @param pri: 消息域
         * @param msg: 消息正文
         * @param member: 群聊成员，非建群报文则为 null
         * @return: JSON 对象
         */
        private JSONObject createJSON(String category, String pri, String msg, JSONArray member, String master) {
            JSONObject data = new JSONObject();
            data.put("category", category);
            data.put("private", pri);
            data.put("msg", msg);
            data.put("member", member);
            data.put("master", master);
            return data;
        }

        /**
         * 函数名称: sendMessage
         * 函数功能: 将封装好的 JSON 对象发给用户
         * @param i: 发送对象为 user_list[i]
         * @param data: JSON 对象
         */
        private void sendMessage(int i, JSONObject data) {
            User user = user_list.get(i);
            try {
                outputStream = new DataOutputStream(user.getSocket().getOutputStream());
                outputStream.writeUTF(data.toString());

            } catch (IOException e) {
                handleUserOffline(i);
            }
        }

        /**
         * 函数名称: handleUserOffline
         * 函数功能: 处理下线用户
         * @param i: 下线用户下标
         */
        private void handleUserOffline(int i) {
            User out_user = user_list.get(i);
            user_list.remove(i);
            username_list.remove(out_user.getUsername());

            JSONObject offline = new JSONObject();
            offline.put("category", "update");
            offline.put("private", "聊天室");
            offline.put("msg", out_user.getUsername() + " 下线了");
            offline.put("user_list", username_list);

            for (User u : user_list) {
                try {
                    outputStream = new DataOutputStream(u.getSocket().getOutputStream());
                    outputStream.writeUTF(offline.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


}

