//import jdk.javadoc.internal.tool.Start;
//import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class Chat2 extends JFrame{
    Socket socket;

    JLabel chatTitle;

    JPanel thechatPanel;

    static String currentChatObject;

    Map<String, List<String>> groupMap = new HashMap<>();

    Map<String, String> groupMaster = new HashMap<>();

    static Map<String, StyledDocument> messageMap = new HashMap<>();

    static JTextPane message_area = new JTextPane();

    static Style style = message_area.addStyle("icon", null);

    DataOutputStream outputStream;

    DataInputStream inputStream;

    String username;

    List<String> userList = new ArrayList<>();

    List<String> groupList = new ArrayList<>();

    JLabel onlineTitle = new JLabel();

    JList<String> list = new JList<>();

    JPopupMenu masterMenu = createMasterMenu();

    JPopupMenu memberMenu = createMemberMenu();

    ClientFileThread fileThread;

    MouseListener currentMouseListener;

    public Chat2(final String username) {

        // 创建文件传输线程
        fileThread = new ClientFileThread(username);
        fileThread.start();

        this.username = username;
        currentChatObject = "聊天室";
        StyledDocument newDoc = new DefaultStyledDocument();
        messageMap.put(currentChatObject, newDoc);
        // 显示信息发送区域
        sendMessagePanel();
        // 显示聊天区域
        chatPanelFiled();
        // 显示在线用户列表
        showOnlineUser();

        setLayout(null);
        setSize(800, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);

        // 获取数据
        JSONObject data = new JSONObject();
        data.put("username", username);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String time = sdf.format(new Date());
        data.put("time", time);

        // 设置窗口标题
        setTitle("ChatRoom   " + username);
        // 设置应用图标
        BufferedImage img;
        try {
            img = ImageIO.read(Objects.requireNonNull(Server.class.getResource("/ChatRoom.png")));
            this.setIconImage(img);

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // 与服务器建立 socket 连接
        try {
            socket = new Socket("localhost", 11111);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(data.toString());
            new Thread(new Read()).start();  // 创建 read 线程
            System.out.println("建立连接成功");

        } catch (IOException e) {
            try {
                StyledDocument doc = new DefaultStyledDocument();
                doc.insertString(doc.getLength(), "服务器无响应", null);
                message_area.setDocument(doc);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null,"服务器无响应","提示", JOptionPane.WARNING_MESSAGE);
        }

    }

    /**
     * 函数名: chatPanelFiled
     * 函数功能: 聊天框样式
     */
    private void chatPanelFiled() {
        thechatPanel = new JPanel();
        thechatPanel.setLayout(new BorderLayout());
        JScrollPane chatScrollPanel = new JScrollPane(message_area,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        thechatPanel.add(chatScrollPanel,BorderLayout.CENTER);
        message_area.setEditable(false);
        chatScrollPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 6));

        chatTitle=new JLabel("聊天室",SwingConstants.CENTER);
        chatTitle.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        thechatPanel.add(chatTitle,BorderLayout.NORTH);

        thechatPanel.setBackground(new Color(125, 186, 255));
        thechatPanel.setBounds(150,5,645,350);
        add(thechatPanel);

    }

    /**
     * 函数名: sendMessagePanel
     * 函数功能: 信息发送区域（右侧）
     */
    private void sendMessagePanel() {
        final JPanel panel_south = new JPanel();
        panel_south.setLayout(new BorderLayout());
        panel_south.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 6));

        // 1.消息编辑区域
        JTextArea send_area = new JTextArea();
        send_area.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 3));
        send_area.setBackground(new Color(240,240,240));
        panel_south.add(send_area, BorderLayout.CENTER);
        // 发送消息按钮
        JButton send_btn = new JButton("发送");
        send_btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        panel_south.add(send_btn, BorderLayout.EAST);

        // 2.功能按钮区域
        JPanel functionButtonPanel = new JPanel(new GridLayout(5,1,5,5));
        // 查看群聊按钮
        JButton groupChatButton = new JButton("聊天室");
        groupChatButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        // 发送图片按钮
        JButton pictureButton = new JButton("发送图片");
        pictureButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        // 发送文件按钮
        JButton fileButton = new JButton("发送文件");
        fileButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        // 创建群聊按钮
        JButton createGroupChatButton = new JButton("发起群聊");
        createGroupChatButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        // 退出按钮
        JButton exitButton = new JButton("退出");
        exitButton.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        // 功能按钮区域样式设置
        functionButtonPanel.add(groupChatButton);
        functionButtonPanel.add(pictureButton);
        functionButtonPanel.add(fileButton);
        functionButtonPanel.add(createGroupChatButton);
        functionButtonPanel.add(exitButton);
        functionButtonPanel.setBackground(new Color(255,255,255));
        panel_south.add(functionButtonPanel,BorderLayout.WEST);
        panel_south.setBackground(new Color(255, 255, 255));
        panel_south.setBounds(150,360,645,155);
        add(panel_south);

        // 聊天室按钮点击事件
        groupChatButton.addActionListener(e -> StartPrivateChat("聊天室"));

        // 创建群聊按钮点击事件
        createGroupChatButton.addActionListener(e -> {
            try {
                JFrame frame = new JFrame("创建群聊");
                frame.setSize(300, 400);
                frame.setLocationRelativeTo(null);

                // 滚动列表
                JScrollPane scroll = new JScrollPane();
                scroll.setBounds(10, 10, 280, 340);
                frame.add(scroll);

                // 显示除自己外的所有在线用户
                DefaultListModel<String> model = new DefaultListModel<>();
                for (String user : userList) {
                    if (!Objects.equals(user, username)) {
                        model.addElement(user);
                    }
                }
                JList<String> groupChatObjList = new JList<>(model);
                groupChatObjList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                scroll.setViewportView(groupChatObjList);

                // 确定按钮
                JButton confirmButton = new JButton("确定");
                confirmButton.setPreferredSize(new Dimension(80, 30));
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
                buttonPanel.add(confirmButton);
                frame.add(buttonPanel, BorderLayout.SOUTH);

                // 第一次点击确定：勾选群聊成员
                confirmButton.addActionListener(e1 -> {
                    if (groupChatObjList.getSelectedValuesList().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "请勾选至少一位群聊成员", "提示", JOptionPane.WARNING_MESSAGE);

                    } else {
                        List<String> selectedUsers = groupChatObjList.getSelectedValuesList();  // 获取勾选用户名
                        selectedUsers.forEach(System.out::println);

                        // 输入群聊名称对话框
                        JDialog dialog = new JDialog(frame, "创建群聊", true);
                        JLabel label = new JLabel("请输入群聊名称：");
                        JTextField textField = new JTextField();
                        textField.setColumns(20);
                        JButton okButton = new JButton("确定");

                        // 第二次点击确定：输入群聊名称
                        okButton.addActionListener(e2 -> {
                            String groupName = textField.getText();
                            if (groupName.isEmpty()) {
                                JOptionPane.showMessageDialog(dialog, "群聊名称不能为空", "提示", JOptionPane.WARNING_MESSAGE);

                            } else {
                                // 发送建立群聊报文给服务器
                                JSONArray temp = new JSONArray();
                                temp.add(username);
                                temp.addAll(selectedUsers);
                                sendMessageToServer("createGroup", username, groupName, temp, "create", "", "");

                                // 发送建立群聊报文给文件服务线程
                                sendMessageToFileServerThread("createGroup", groupName, temp);

                                dialog.dispose();
                                frame.dispose();

                                // 自己也要更新
                                groupList.add(groupName);
                                selectedUsers.add(username);
                                groupMap.put(groupName, selectedUsers);
                                groupMaster.put(groupName, username);
                                currentChatObject = groupName;

                                // 刷新左边和右边
                                getOnlineUserOrGroup("Group", groupList.size());
                                StartPrivateChat(groupName);

                                // 更新 doc 与 messageMap
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                String time = sdf.format(new Date());     // 获取当前时间
                                String text = (time + " " + username + "，您成功建立了群聊“" + groupName + "”，" + "群内成员有：" + selectedUsers + "\n");
                                StyledDocument doc = message_area.getStyledDocument();
                                try {
                                    doc.insertString(doc.getLength(), text, null);

                                } catch (BadLocationException ex) {
                                    ex.printStackTrace();
                                }
                                messageMap.replace(groupName, doc);
                            }
                        });

                        dialog.setLayout(new FlowLayout());
                        dialog.add(label);
                        dialog.add(textField);
                        dialog.add(okButton);
                        dialog.pack();
                        dialog.setLocationRelativeTo(null);
                        dialog.setVisible(true);
                        frame.dispose(); // 关闭窗口
                    }
                });
                frame.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 发送图片按钮点击事件
        pictureButton.addActionListener(e -> showGraphOpenDialog());

        // 发送文件按钮点击事件
        fileButton.addActionListener(e -> showFileOpenDialog());

        // 发送消息按钮点击事件
        send_btn.addActionListener(e -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String time = sdf.format(new Date());     // 获取当前时间
                String msg = send_area.getText().trim();  // 获取输入文本

                if (!msg.isEmpty()) {
                    String text = time + " " + username + "\n" + msg + "\n";
                    StyledDocument doc = message_area.getStyledDocument();
                    try {
                        doc.insertString(doc.getLength(), text, null);

                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                    messageMap.replace(currentChatObject, doc);

                    if (!Objects.equals(currentChatObject, username)) {
                        if (groupList.contains(currentChatObject)) {  // 群聊报文
                            sendMessageToServer("group", username, currentChatObject, null, "", time, msg);

                        } else if (Objects.equals(currentChatObject, "聊天室")) {  // 聊天室报文
                            sendMessageToServer("all", username, currentChatObject, null, "", time, msg);

                        } else {  // 私聊报文
                            sendMessageToServer("user", username, currentChatObject, null, "", time, msg);
                        }
                    }

                }
                send_area.setText("");

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // 退出按钮点击事件
        exitButton.addActionListener(e ->{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String time = sdf.format(new Date());
            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("msg", "下线");
            data.put("time", time);
            data.put("category","all");
            try {
                outputStream.writeUTF(data.toString());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            try {
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.exit(0);
        });

    }


    /**
     * 函数名: showFileOpenDialog
     * 函数功能: 发送文件按钮点击事件调用函数
     */
    private void showFileOpenDialog() {
        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser();
        // 设置默认显示的文件夹
        fileChooser.setCurrentDirectory(new File("C:/Users/lenovo/Desktop"));
        // 设置文件过滤器
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt", "doc", "docx"));
        // 打开文件选择框
        int result = fileChooser.showOpenDialog(new JPanel());
        // 点击确定
        if (result == JFileChooser.APPROVE_OPTION) {
            // 获取文件路径
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            String fileName = file.getName();
            System.out.println("发送文件的路径为 " + path);
            System.out.println("发送文件的文件名为 " + fileName);
            fileThread.outFileToServer(path, currentChatObject, "file");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String time = sdf.format(new Date());
            String text = time + " " + username + "\n" + "发送了文件“" + fileName + "”\n";
            StyledDocument doc = message_area.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), text, null);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            messageMap.replace(currentChatObject, doc);
        }
    }

    /**
     * 函数名: showFileOpenDialog
     * 函数功能: 发送图片按钮点击事件调用函数
     */
    private void showGraphOpenDialog() {
        // 创建文件选择器
        JFileChooser fileChooser = new JFileChooser();
        // 设置默认显示的文件夹
        fileChooser.setCurrentDirectory(new File("C:/Users/lenovo/Desktop"));
        // 设置文件过滤器
        fileChooser.setFileFilter(new FileNameExtensionFilter("图像文件", "jpg", "jpeg", "png", "gif"));
        // 打开文件选择框
        int result = fileChooser.showOpenDialog(new JPanel());
        // 点击确定
        if (result == JFileChooser.APPROVE_OPTION) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String time = sdf.format(new Date());
            String text = time + " " + username + "\n";
            StyledDocument doc = message_area.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), text, null);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }

            // 获取文件路径
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            String fileName = file.getName();
            System.out.println("发送图片的路径为 " + path);
            System.out.println("发送图片的文件名为 " + fileName);
            // 加载图像文件
            ImageIcon icon = new ImageIcon(path);
            Image image = icon.getImage();

            // 缩放图像
            int width = message_area.getWidth();
            int height = message_area.getHeight();
            double scaleX = (double) width / image.getWidth(null);
            double scaleY = (double) height / image.getHeight(null);
            double scale = Math.min(scaleX, scaleY);
            int scaledWidth = (int) (image.getWidth(null) * scale / 2);
            int scaleHeight = (int) (image.getHeight(null) * scale / 2);
            Image scaledImage = image.getScaledInstance(scaledWidth, scaleHeight, Image.SCALE_SMOOTH);

            // 将图像添加到 doc 中，更新 messageMap
            Icon icon2 = new ImageIcon(scaledImage);
            StyleConstants.setIcon(style, icon2);
            try {
                doc.insertString(doc.getLength(), "\n", style);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            messageMap.replace(currentChatObject, doc);

            // 发送图像文件到服务器
            if (!Objects.equals(currentChatObject, username)) {
                fileThread.outFileToServer(path, currentChatObject, "image");
            }
        }
    }

    /**
     * 函数名: showOnlineUser
     * 函数功能: 客户端首次登陆时，设置左侧列表的样式
     */
    private void showOnlineUser() {
        System.out.println("调用 Chat2 类 showOnlineUser 函数");

        // 首次进入界面，显示在线用户列表
        getOnlineUserOrGroup("User", 1);

        // 设置样式
        list.setFont(new Font("微软雅黑", Font.BOLD, 13));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton onlineUserButton = new JButton("用户");
        JButton groupChatButton = new JButton("群聊");
        buttonPanel.add(onlineUserButton);
        buttonPanel.add(Box.createHorizontalStrut(18)); //添加一个水平间距
        buttonPanel.add(groupChatButton);

        onlineUserButton.addActionListener(e -> getOnlineUserOrGroup("User", userList.size()));
        groupChatButton.addActionListener(e -> getOnlineUserOrGroup("Group", groupList.size()));

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        listPanel.add(list, BorderLayout.CENTER);
        onlineTitle.setHorizontalAlignment(SwingConstants.CENTER);
        onlineTitle.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        listPanel.add(onlineTitle,BorderLayout.NORTH);
        listPanel.setBounds(5,5,140,510);
        listPanel.setBackground(new Color(255, 160, 125));
        add(listPanel);

    }

    /**
     * 函数名称: getOnlineUserOrGroup
     * 函数功能: 刷新左侧列表
     * @param flag: 显示在线用户 or 已加入的群聊
     * @param size: 在线人数 or 群聊数
     */
    private void getOnlineUserOrGroup(String flag, int size) {
        System.out.println("调用 Chat2 类 getOnlineUser 函数 " + flag + " " + size);

        DefaultListModel<String> DModel = new DefaultListModel<>();

        // 显示在线用户列表
        if (Objects.equals(flag, "User")) {
            for (String s : userList) {
                DModel.addElement(s);
            }
            onlineTitle.setText("在线用户 [" + size + "]");

            // 添加鼠标点击事件监听器
            list.removeMouseListener(currentMouseListener);
            currentMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    if (list.getSelectedIndex() != -1) {
                        if (e.getClickCount() == 2) {  // 双击用户进入私聊
                            String target = list.getSelectedValue();
                            System.out.println("双击了" + target);
                            StartPrivateChat(target);
                        }

                    }
                }
            };
            list.addMouseListener(currentMouseListener);

        // 显示已加入的群聊列表
        } else if (Objects.equals(flag, "Group")) {
            for (String s : groupList) {
                DModel.addElement(s);
            }
            onlineTitle.setText("群聊 [" + size + "]");

            // 添加鼠标点击事件监听器
            list.removeMouseListener(currentMouseListener);
            currentMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    if (list.getSelectedIndex() != -1) {
                        if (e.getClickCount() == 2) {  // 双击进入群聊
                            String target = list.getSelectedValue();
                            System.out.println("双击了" + target);
                            StartPrivateChat(target);
                        }

                        if (e.getButton() == 3 && Objects.equals(list.getSelectedValue(), currentChatObject)) {  // 右键单击，弹出菜单
                            String item = list.getSelectedValue();
                            System.out.println("右键单击了" + item + "，当前域为：" + currentChatObject);
                            if (Objects.equals(groupMaster.get(item), username)) {  // 群主
                                masterMenu.show(list, e.getX(), e.getY());

                            } else {  // 非群主
                                memberMenu.show(list, e.getX(), e.getY());
                            }
                        }
                    }
                }
            };
            list.addMouseListener(currentMouseListener);

        }

        list.setModel(DModel);
    }

    /**
     * 函数名: createMasterMenu
     * 函数功能: 群主右键单击群聊时的弹出菜单
     * @return: masterMenu
     */
    private JPopupMenu createMasterMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem[] menuItems = new JMenuItem[4];
        menuItems[0] = new JMenuItem("查看群成员");
        menuItems[1] = new JMenuItem("添加成员");
        menuItems[2] = new JMenuItem("删除成员");
        menuItems[3] = new JMenuItem("解散群聊");
        for (JMenuItem menuItem : menuItems) {
            menuItem.setFont(new Font("微软雅黑", Font.BOLD, 13));
            menu.add(menuItem);

            // 添加鼠标点击事件
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String option = menuItem.getText();
                    System.out.println("选择了" + option);
                    switch (option) {
                        case "查看群成员":
                            JFrame frame = new JFrame("群聊所有成员");
                            frame.setSize(300, 400);
                            frame.setLocationRelativeTo(null);

                            // 滚动列表
                            JScrollPane scroll = new JScrollPane();
                            scroll.setBounds(10, 10, 280, 340);
                            frame.add(scroll);

                            // 显示所有群聊用户
                            DefaultListModel<String> model = new DefaultListModel<>();
                            for (String user : groupMap.get(currentChatObject)) {
                                model.addElement(user);
                            }
                            JList<String> groupChatMember = new JList<>(model);

                            // 添加鼠标点击事件
                            groupChatMember.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    super.mouseClicked(e);
                                    if (groupChatMember.getSelectedIndex() != -1) {
                                        if (e.getClickCount() == 2) {  // 双击进入私聊
                                            String target = groupChatMember.getSelectedValue();
                                            System.out.println("双击了" + target);
                                            StartPrivateChat(target);
                                            frame.dispose();
                                        }
                                    }
                                }
                            });
                            scroll.setViewportView(groupChatMember);
                            frame.setVisible(true);
                            break;

                        case "添加成员":
                            JFrame frame1 = new JFrame("请选择要添加的成员");
                            frame1.setSize(300, 400);
                            frame1.setLocationRelativeTo(null);

                            // 滚动列表
                            JScrollPane scroll1 = new JScrollPane();
                            scroll1.setBounds(10, 10, 280, 340);
                            frame1.add(scroll1);

                            // 显示不在群聊内的所有用户
                            DefaultListModel<String> model1 = new DefaultListModel<>();
                            for (String user : userList) {
                                if (!groupMap.get(currentChatObject).contains(user)) {
                                    model1.addElement(user);
                                }
                            }
                            JList<String> addMember = new JList<>(model1);
                            addMember.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                            scroll1.setViewportView(addMember);

                            // 确定按钮
                            JButton confirmButton = new JButton("确定");
                            confirmButton.setPreferredSize(new Dimension(80, 30));
                            JPanel buttonPanel = new JPanel();
                            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
                            buttonPanel.add(confirmButton);
                            frame1.add(buttonPanel, BorderLayout.SOUTH);

                            // 点击确定：勾选要添加的成员
                            confirmButton.addActionListener(e1 -> {
                                if (addMember.getSelectedValuesList().isEmpty()) {
                                    JOptionPane.showMessageDialog(null, "请勾选至少一位成员", "提示", JOptionPane.WARNING_MESSAGE);

                                } else {
                                    // 发送添加群聊成员报文给服务器
                                    List<String> selectedUsers = addMember.getSelectedValuesList();  // 获取勾选用户名
                                    JSONArray temp = new JSONArray();
                                    temp.addAll(selectedUsers);
                                    sendMessageToServer("createGroup", username, currentChatObject, temp, "add", "", "");

                                    // 发送添加群聊成员报文给文件服务线程
                                    sendMessageToFileServerThread("addMember", currentChatObject, temp);

                                    frame1.dispose();
                                }
                            });
                            frame1.setVisible(true);
                            break;

                        case "删除成员":
                            JFrame frame2 = new JFrame("请选择要删除的成员");
                            frame2.setSize(300, 400);
                            frame2.setLocationRelativeTo(null);

                            // 滚动列表
                            JScrollPane scroll2 = new JScrollPane();
                            scroll2.setBounds(10, 10, 280, 340);
                            frame2.add(scroll2);

                            // 显示群聊内除群主外的所有用户
                            DefaultListModel<String> model2 = new DefaultListModel<>();
                            for (String user : userList) {
                                if (groupMap.get(currentChatObject).contains(user) && !Objects.equals(user, username)) {
                                    model2.addElement(user);
                                }
                            }
                            JList<String> deleteMember = new JList<>(model2);
                            deleteMember.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                            scroll2.setViewportView(deleteMember);

                            // 确定按钮
                            JButton confirmButton2 = new JButton("确定");
                            confirmButton2.setPreferredSize(new Dimension(80, 30));
                            JPanel buttonPanel2 = new JPanel();
                            buttonPanel2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 10));
                            buttonPanel2.add(confirmButton2);
                            frame2.add(buttonPanel2, BorderLayout.SOUTH);

                            // 点击确定：勾选要删除的成员
                            confirmButton2.addActionListener(e1 -> {
                                if (deleteMember.getSelectedValuesList().isEmpty()) {
                                    JOptionPane.showMessageDialog(null, "请勾选至少一位成员", "提示", JOptionPane.WARNING_MESSAGE);

                                } else {
                                    // 发送删除群聊成员报文给服务器
                                    List<String> selectedUsers = deleteMember.getSelectedValuesList();  // 获取勾选用户名
                                    JSONArray temp = new JSONArray();
                                    temp.addAll(selectedUsers);
                                    sendMessageToServer("createGroup", username, currentChatObject, temp, "delete", "", "");

                                    // 发送删除群聊成员报文给文件服务线程
                                    sendMessageToFileServerThread("deleteMember", currentChatObject, temp);

                                    frame2.dispose();
                                }
                            });
                            frame2.setVisible(true);
                            break;

                        case "解散群聊":
                            int result = JOptionPane.showConfirmDialog(new JPanel(), "确定解散群聊吗？", "提示", JOptionPane.YES_NO_OPTION);
                            if (result == 0) {
                                // 发送解散群聊报文给服务器
                                sendMessageToServer("createGroup", username, currentChatObject, null, "clear", "", "");

                                // 发送解散群聊报文给文件服务线程
                                sendMessageToFileServerThread("clearGroup", currentChatObject, null);
                            }

                            break;
                    }
                }
            });
        }
        return menu;
    }

    /**
     * 函数名: createMemberMenu
     * 函数功能: 非群主成员右键单击群聊时的弹出菜单
     * @return: memberMenu
     */
    private JPopupMenu createMemberMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem[] menuItems = new JMenuItem[2];
        menuItems[0] = new JMenuItem("查看群成员");
        menuItems[1] = new JMenuItem("退出群聊");
        for (JMenuItem menuItem : menuItems) {
            menuItem.setFont(new Font("微软雅黑", Font.BOLD, 13));
            menu.add(menuItem);

            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String option = menuItem.getText();
                    System.out.println("选择了" + option);
                    switch (option) {
                        case "查看群成员":
                            JFrame frame = new JFrame("群聊所有成员");
                            frame.setSize(300, 400);
                            frame.setLocationRelativeTo(null);

                            // 滚动列表
                            JScrollPane scroll = new JScrollPane();
                            scroll.setBounds(10, 10, 280, 340);
                            frame.add(scroll);

                            // 显示所有群聊用户
                            DefaultListModel<String> model = new DefaultListModel<>();
                            for (String user : groupMap.get(currentChatObject)) {
                                model.addElement(user);
                            }

                            // 添加鼠标点击事件
                            JList<String> groupChatMember = new JList<>(model);
                            groupChatMember.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    super.mouseClicked(e);
                                    if (groupChatMember.getSelectedIndex() != -1) {
                                        if (e.getClickCount() == 2) {  // 双击进入私聊
                                            String target = groupChatMember.getSelectedValue();
                                            System.out.println("双击了" + target);
                                            StartPrivateChat(target);
                                            frame.dispose();
                                        }
                                    }
                                }
                            });
                            scroll.setViewportView(groupChatMember);
                            frame.setVisible(true);
                            break;

                        case "退出群聊":
                            int result = JOptionPane.showConfirmDialog(new JPanel(), "确定退出群聊吗？", "提示", JOptionPane.YES_NO_OPTION);
                            if (result == 0) {

                                // 发送退出群聊报文给服务器
                                sendMessageToServer("createGroup", username, currentChatObject, null, "bye", "", "");

                                // 发送群聊成员退出报文给文件服务线程
                                JSONArray member = new JSONArray();
                                member.add(username);
                                sendMessageToFileServerThread("deleteMember", currentChatObject, member);

                                // 清楚群聊信息
                                byeToGroup(currentChatObject);
                            }
                            break;
                    }
                }
            });
        }
        return menu;
    }

    /**
     * 函数名称: StartPrivateChat
     * 函数功能: 进入聊天域（刷新右侧界面）
     * @param chatPeople: 聊天域
     */
    private void StartPrivateChat(String chatPeople) {
        System.out.println("调用 Chat2 类 StartPrivateChat 函数 " + chatPeople);
        currentChatObject = chatPeople;
        chatTitle.setText(chatPeople);
        thechatPanel.setBackground(new Color(125, 255, 197));

        StyledDocument doc;
        if (messageMap.get(currentChatObject) == null) {
            doc = new DefaultStyledDocument();
            message_area.setDocument(doc);
            messageMap.put(currentChatObject, doc);

        } else {
            doc = messageMap.get(currentChatObject);
            message_area.setDocument(doc);
        }

    }

    /**
     * 类名: Read
     * 类功能: 处理服务器发来的报文的线程
     */
    public class Read implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String json = inputStream.readUTF();
                    JSONObject data = JSONObject.fromObject(json);
                    String cat = data.getString("category");
                    String pri = data.getString("private");

//                    if (msg.contains("踢出") && msg.contains(username)) {  // 被踢报文
//                        System.out.println("收到被踢报文");
//                        is_stop = true;
//                        message_area.append(username + ",你已经被踢出群聊\n");
//                        JOptionPane.showMessageDialog(null,"你已经被踢出群聊","提示",
//                                JOptionPane.WARNING_MESSAGE);
//                        System.exit(0);

                    if (Objects.equals(cat, "createGroup")) {  // 建立群聊报文 or 添加新成员报文-发给新成员
                        System.out.println("收到 createGroup 报文");
                        currentChatObject = pri;
                        groupList.add(pri);

                        // 刷新左边和右边
                        getOnlineUserOrGroup("Group", groupList.size());
                        StartPrivateChat(pri);

                        String text = ("您已被" + data.getString("master") + "拉入群聊 " + pri + "\n" + data.getString("msg") + "\n");
                        StyledDocument doc = message_area.getStyledDocument();
                        try {
                            doc.insertString(doc.getLength(), text, null);

                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                        messageMap.replace(pri, doc);

                        // 更新 groupMap 和 groupMaster
                        JSONArray member = data.getJSONArray("member");
                        List<String> memberList = new ArrayList<>();
                        for (int i = 0; i < member.size(); i++) {
                            memberList.add(member.getString(i));
                        }
                        groupMap.put(pri, memberList);
                        groupMaster.put(pri, data.getString("master"));

                    } else if (Objects.equals(cat, "deleteGroup")) {  // 删除群聊成员报文-发给被删成员
                        System.out.println("收到 deleteGroup 报文");
                        JOptionPane.showMessageDialog(new JPanel(), "您已被踢出群聊" + pri, "提示", JOptionPane.INFORMATION_MESSAGE);
                        byeToGroup(pri);

                    }  else if (Objects.equals(cat, "clearGroup")) {  // 解散群聊报文
                        System.out.println("收到 clearGroup 报文");
                        JOptionPane.showMessageDialog(new JPanel(), "群主解散了群聊" + pri, "提示", JOptionPane.INFORMATION_MESSAGE);
                        byeToGroup(pri);

                    } else if (Objects.equals(cat, "updateGroup")) {  // 添加新成员报文-发给其他成员 or 删除群聊成员报文-发给其它成员 or 退出群聊报文
                        System.out.println("收到 updateGroup 报文");

                        // 更新 groupMap
                        JSONArray member = data.getJSONArray("member");
                        List<String> memberList = new ArrayList<>();
                        for (int i = 0; i < member.size(); i++) {
                            memberList.add(member.getString(i));
                        }
                        groupMap.replace(pri, memberList);
                        handleMessage(pri, data.getString("msg"));

                    } else if (Objects.equals(cat, "user")) {  // 正常消息报文

                        if (data.getString("msg").contains("踢出") && data.getString("msg").contains(username)) {  // 被踢报文
                            System.out.println("收到被踢报文");
                            System.exit(0);
                        }else if(data.getString("msg").contains("踢出")||data.getString("msg").contains("下线")){

                            // 更新用户列表
                            userList.clear();
                            JSONArray jsonArray = data.getJSONArray("user_list");
                            for (Object o : jsonArray) {
                                userList.add(o.toString());
                            }
                            getOnlineUserOrGroup("User", jsonArray.size());
                            handleMessage("聊天室", data.getString("msg"));
                        }
                        else {
                            System.out.println("收到正常消息报文，目标域为：" + pri + "，当前域为：" + currentChatObject);
                            handleMessage(pri, data.getString("msg"));
                        }

                    } else if (Objects.equals(cat, "update")) {  // 用户上下线报文
                        System.out.println("收到用户上下线报文，目标域为：" + pri + "，当前域为：" + currentChatObject);
                        handleMessage(pri, data.getString("msg"));

                        // 更新用户列表
                        userList.clear();
                        JSONArray jsonArray = data.getJSONArray("user_list");
                        for (Object o : jsonArray) {
                            userList.add(o.toString());
                        }
                        getOnlineUserOrGroup("User", jsonArray.size());

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 函数名称: handleMessage
     * 函数功能: 更新 messageMap ；如果目标消息域是当前消息域，则更新 message_area
     * @param pri: 消息域
     * @param msg: 消息正文
     */
    private void handleMessage(String pri, String msg) {
        // 更新 messageMap
        String text = msg + "\n";
        StyledDocument doc;

        if (messageMap.get(pri) == null) {
            doc = new DefaultStyledDocument();
            try {
                doc.insertString(0, text, null);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            messageMap.put(pri, doc);

        } else {
            doc = messageMap.get(pri);
            try {
                doc.insertString(doc.getLength(), text, null);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            messageMap.replace(pri, doc);
        }

        // 更新 message_area
        if (Objects.equals(pri, currentChatObject)) {
            message_area.setDocument(doc);
        }
    }

    /**
     * 函数名称: byeToGroup
     * 函数功能: 从 groupList 、GroupMap 、groupMaster 、messageMap 中删除群聊信息
     * @param pri: 群聊名称
     */
    private void byeToGroup(String pri) {
        groupList.remove(pri);
        groupMap.remove(pri);
        groupMaster.remove(pri);
        messageMap.remove(pri);

        // 刷新左边和右边
        getOnlineUserOrGroup("Group", groupList.size());
        if (Objects.equals(currentChatObject, pri)) {
            currentChatObject = "聊天室";
            StartPrivateChat(currentChatObject);
        }
    }

    /**
     * 函数名称: sendMessageToServer
     * 函数功能: 发送报文到服务器
     * @param cat:
     * @param un:
     * @param pri:
     * @param member:
     * @param flag:
     * @param time:
     * @param msg:
     */
    private void sendMessageToServer(String cat, String un, String pri, JSONArray member, String flag, String time, String msg) {
        JSONObject data = new JSONObject();
        data.put("category", cat);
        if (!Objects.equals(un, "")) {
            data.put("username", un);
        }
        if (!Objects.equals(pri, "")) {
            data.put("private", pri);
        }
        if (member != null) {
            data.put("member", member);
        }
        if (!Objects.equals(flag, "")) {
            data.put("flag", flag);
        }
        if (!Objects.equals(time, "")) {
            data.put("time", time);
        }
        if (!Objects.equals(msg, "")) {
            data.put("msg", msg);
        }

        try {
            outputStream.writeUTF(data.toString());

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 函数名称:
     * 函数功能: 发送报文到文件服务线程
     * @param category:
     * @param groupName:
     * @param member:
     */
    private void sendMessageToFileServerThread(String category, String groupName, JSONArray member) {

        Socket socketFile;
        try {
            socketFile = new Socket("localhost", 8090);
            DataOutputStream outputStreamFile = new DataOutputStream(socketFile.getOutputStream());
            JSONObject dataFile = new JSONObject();
            dataFile.put("category", category);
            dataFile.put("group", groupName);
            if (member != null) {
                dataFile.put("member", member);
            }
            outputStreamFile.writeUTF(dataFile.toString());

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}