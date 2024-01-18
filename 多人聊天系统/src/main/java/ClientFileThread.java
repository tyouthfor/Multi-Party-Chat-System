// ClientFileThread.java

import net.sf.json.JSONObject;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * 类名: ClientFileThread
 * 类功能: 客户端文件传输线程
 */
public class ClientFileThread extends Thread{
    static String userName = null;
    static DataInputStream fileIn = null;
    static DataOutputStream fileOut = null;
    static DataInputStream fileReader = null;
    static DataOutputStream fileWriter = null;

    public ClientFileThread(String userName) {
        ClientFileThread.userName = userName;
    }

    /**
     * 函数名称: run
     * 函数功能: 与服务器的文件服务线程建立连接，并接收服务器发来的文件
     */
    public void run() {
        try {
            Socket socket = new Socket("localhost", 8090);  // 与文件服务线程建立 socket 连接
            fileIn = new DataInputStream(socket.getInputStream());
            fileOut = new DataOutputStream(socket.getOutputStream());

            // 发送 username ，让文件服务线程创建用户
            JSONObject data = new JSONObject();
            data.put("category", "createUser");
            data.put("username", userName);
            fileOut.writeUTF(data.toString());

            // 接收文件
            while (true) {
                String textName = fileIn.readUTF();    // 文件名
                String pri = fileIn.readUTF();         // 发送域
                String people = fileIn.readUTF();      // 发送者
                String category = fileIn.readUTF();    // 文件类型：文件 or 图像
                String cat = fileIn.readUTF();         // 发送类型：聊天室 or 群聊 or 私聊
                long totalLength = fileIn.readLong();  // 文件长度
                System.out.println("收到文件，文件名为：" + textName + "，发送域为：" + pri + "，发送者为：" + people + "，类型为：" + category + "，文件长度为：" + totalLength);

                int result = 1;
                if (category.equals("file")) {
                    if (cat.equals("私聊")) {
                        result = JOptionPane.showConfirmDialog(new JPanel(), people + "向你悄悄发了一个文件，是否接收？", "提示", JOptionPane.YES_NO_OPTION);

                    } else if (cat.equals("聊天室") || cat.equals("群聊")) {
                        result = JOptionPane.showConfirmDialog(new JPanel(), people + "在" + pri + "里发送了一个文件，是否接收？", "提示", JOptionPane.YES_NO_OPTION);

                    } else {
                        System.out.println("发送类型错误");
                    }

                } else if (category.equals("image")) {
                    if (cat.equals("私聊")) {
                        result = JOptionPane.showConfirmDialog(new JPanel(), people + "向你悄悄发了一张图片，是否接收？", "提示", JOptionPane.YES_NO_OPTION);

                    } else if (cat.equals("聊天室") || cat.equals("群聊")) {
                        result = JOptionPane.showConfirmDialog(new JPanel(), people + "在" + pri + "里发送了一张图片，是否接收？", "提示", JOptionPane.YES_NO_OPTION);

                    } else {
                        System.out.println("发送类型错误");
                    }
                }

                int length;
                byte[] buff = new byte[1024];
                long curLength = 0;

                if (result == 0) {  // 接受文件
                    // 确认指定接收文件的目录存在
                    File userFile = new File("C:\\Users\\lenovo\\Desktop\\接收文件" + userName);
                    if (!userFile.exists()) {
                        boolean bol = userFile.mkdir();  // 如果路径不存在，就新建此目录
                        if (!bol) {
                            System.out.println("新建目录失败");
                        }
                    }

                    // 将文件写入本地目录下
                    String path = ("C:\\Users\\lenovo\\Desktop\\接收文件" + userName + "\\" + textName);
                    File file = new File(path);
                    fileWriter = new DataOutputStream(Files.newOutputStream(file.toPath()));
                    while ((length = fileIn.read(buff)) > 0) {
                        fileWriter.write(buff, 0, length);
                        fileWriter.flush();
                        curLength += length;
                        if (curLength == totalLength) {
                            break;
                        }
                    }

                    // 显示图像
                    if (category.equals("image")) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        String time = sdf.format(new Date());
                        String text = time + " " + people + "\n";
                        StyledDocument doc;

                        // 私聊域处理
                        if (cat.equals("私聊")) {
                            pri = people;
                        }
                        System.out.println("目标域为：" + pri);

                        if (Chat2.messageMap.containsKey(pri)) {
                            doc = Chat2.messageMap.get(pri);
                            try {
                                doc.insertString(doc.getLength(), text, null);

                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }

                        } else {
                            doc = new DefaultStyledDocument();
                            try {
                                doc.insertString(0, text, null);

                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        }

                        // 加载图像文件
                        ImageIcon icon = new ImageIcon(path);
                        Image image = icon.getImage();

                        // 缩放图像
                        int width = Chat2.message_area.getWidth();
                        int height = Chat2.message_area.getHeight();
                        double scaleX = (double) width / image.getWidth(null);
                        double scaleY = (double) height / image.getHeight(null);
                        double scale = Math.min(scaleX, scaleY);
                        int scaledWidth = (int) (image.getWidth(null) * scale / 2);
                        int scaleHeight = (int) (image.getHeight(null) * scale / 2);
                        Image scaledImage = image.getScaledInstance(scaledWidth, scaleHeight, Image.SCALE_SMOOTH);

                        // 将图像添加到 doc 中，更新 messageMap
                        Icon icon2 = new ImageIcon(scaledImage);
                        StyleConstants.setIcon(Chat2.style, icon2);
                        try {
                            doc.insertString(doc.getLength(), "\n", Chat2.style);

                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                        Chat2.messageMap.replace(pri, doc);

                        // 更新 message_area
                        if (Objects.equals(pri, Chat2.currentChatObject)) {
                            Chat2.message_area.setDocument(doc);
                        }
                    }
                    // 显示发送文件名
                    else {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        String time = sdf.format(new Date());
                        String text = time + " " + people + "\n" + "发送了文件“" + textName +"”\n";
                        StyledDocument doc;

                        // 私聊域处理
                        if (cat.equals("私聊")) {
                            pri = people;
                        }
                        System.out.println("目标域为：" + pri);

                        if (Chat2.messageMap.containsKey(pri)) {
                            doc = Chat2.messageMap.get(pri);
                            try {
                                doc.insertString(doc.getLength(), text, null);

                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }

                        } else {
                            doc = new DefaultStyledDocument();
                            try {
                                doc.insertString(0, text, null);

                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        }

                    }

                    // 弹窗提示
                    if (category.equals("file")) {
                        JOptionPane.showMessageDialog(new JPanel(), "文件存放地址：" + path , "提示", JOptionPane.INFORMATION_MESSAGE);

                    } else {
                        JOptionPane.showMessageDialog(new JPanel(), "图片存放地址：" + path , "提示", JOptionPane.INFORMATION_MESSAGE);
                    }

                } else {  // 不接受文件
                    while ((length = fileIn.read(buff)) > 0) {
                        curLength += length;
                        if (curLength == totalLength) {
                            break;
                        }
                    }
                }
                fileWriter.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 函数名称: outFileToServer
     * 函数功能: 发送文件到服务器
     * @param path: 文件的绝对路径
     * @param currentChatObject: 聊天域
     */
    public void outFileToServer(String path, String currentChatObject, String cat) {
        try {
            System.out.println("客户端发送文件开始，文件路径为：" + path + "，发送域为：" + currentChatObject);
            File file = new File(path);
            fileReader = new DataInputStream(Files.newInputStream(file.toPath()));

            fileOut.writeUTF(file.getName());  // 发送文件名字
            fileOut.flush();

            fileOut.writeUTF(currentChatObject);  // 发送域
            fileOut.flush();

            fileOut.writeUTF(userName);  // 发送者
            fileOut.flush();

            fileOut.writeUTF(cat);  // 普通文件 or 图像文件
            fileOut.flush();

            fileOut.writeLong(file.length());  // 发送文件长度
            fileOut.flush();

            // 读文件并写入 fileOut
            int length;
            byte[] buff = new byte[1024];
            while ((length = fileReader.read(buff)) > 0) {
                fileOut.write(buff, 0, length);
                fileOut.flush();
            }

            // 弹窗提示
            JOptionPane.showMessageDialog(new JPanel(), "成功发送文件！", "提示", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            System.out.println("客户端发送文件失败");
        }
    }
}