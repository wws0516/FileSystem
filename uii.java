package FileSystem;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.DirectoryIteratorException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import FileSystem.tableModel;
import FileSystem.Disk;
import FileSystem.FCB;



/**
 * Created by wws.
 */
public class uii extends JFrame {
    private JTree tree;
    private JScrollPane treePane;
    private JScrollPane tablePane;
    private tableModel model = new tableModel();
    private JTable fileTable;
    private JPopupMenu myMenu = new JPopupMenu();
    private JFileChooser chooser;

    private File rootFile;
    private File readMe;
    private FileWriter readMeWrite;

    private Disk diskA;
    private Disk diskC;
    private Disk diskD;
    private Disk diskE;
    private Map<Character,Disk> disks = new HashMap<Character,Disk>();

    private JLabel diskName = new JLabel("Disk Name:");
    private JLabel nameField = new JLabel();
    private JLabel haveUsed = new JLabel("Used:");
    private JLabel usedField = new JLabel();
    private JLabel freeYet = new JLabel("Free:");
    private JLabel freeField = new JLabel();
    private JLabel fileNum = new JLabel("Disk's File Number:");
    private JLabel fileNumField = new JLabel();

    private JTextField searchLine = new JTextField();

    private static String helpMessage =
            "<html>" +
                    "<body>" +
                    "<h1>文件系统模拟</h1>" +
                    "<h2>技术细节</h2>" +
                    "<h3>显式链接(FAT)</h3>" +
                    "<ul> <li>用一个专用文件记录分配和未分配的内存块</li> <li>使用链接的方式,不存在内存碎片</li> </ul>" +
                    "<h3>空闲空间管理 —— 位图</h3>" +
                    "<ul> <li>用二进制0和1分别代表未分配的内存块和已经分配的内存块</li> <li>在该项目中位图和FAT进行了合并</li> </ul>" +
                    "<h3>目录结构 —— 多级目录结构</h3>" +
                    "<h3>FCB</h3>" +
                    "<ul> <li>文件类型</li> <li>文件名</li> <li>文件大小</li> <li>文件最近更新时间</li> </ul>" +
                    "<h2>操作说明</h2>" +
                    "<ul> <li>必须先选择一个计算机上的文件夹作为模拟工作目录</li> <li>左侧树状结构即文件目录</li> <li>双击或点击文件夹左侧小图标可以打开文件目录</li> " +
                    "<li>右侧空白处为表格区域,将现实相关文件信息</li> <li>双击表格中的项可以直接打开相关文件</li>" +
                    "<li>下方绿色为盘信息面板,将现实相应盘的相应信息</li>" +
                    "<li>最下方空白处将显示内存块当前状况</li> </ul>" +
                    "<li>在树状结构中选中某一节点,右键即可选择相应的文件操作</li>" +
                    "<li>创建新的文件会要求输入文件名和文件大小(KB)</li>" +
                    "<h2>特别说明</h2>" +
                    "<ul> <li>本程序重在模拟,并不是真正地为文件开了这么大的空间</li> <li>仅支持生成txt,文本文件中直接现实FCB,不支持修改内容</li>" +
                    "<li>对于非法输入都会直接导致文件生成失败</li> <li>如果存档文件recover.txt被破坏,将无法打开文件</li></ul>" +
                    "</body>" +
                    "</html>";
    private static File sourceFile;

    // Delete a dir
    public static void deleteDirectory(String filePath){
        File file = new File(filePath);
        if(!file.exists()){
            return;
        }
        if(file.isFile()){
            file.delete();
        }else if(file.isDirectory()){
            File[] files = file.listFiles();
            for (File myfile : files) {
                deleteDirectory(filePath + File.separator + myfile.getName());
            }
            file.delete();
        }
    }

    // Get space
    public double getSpace(File file){
        double space = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine();
            space = Double.parseDouble(reader.readLine());
            if (space > 1024){
                space = 0.0;
            }
            reader.close();
        } catch (Exception e){};
        return space;
    }

    // Update disk's information
    public void upDateDisk(Disk currentDisk){
        fileNumField.setText(String.valueOf(currentDisk.getFileNum()));
        usedField.setText(String.valueOf(currentDisk.getSpace()) + " KB");
        freeField.setText(String.valueOf(1024 - currentDisk.getSpace()) + "KB");
    }

    // Search a file
    public boolean searchFile(String fileName, File parent){
        File [] files = parent.listFiles();
        for (File myFile:files){
            if (myFile.getName().equals(fileName)){
                try {
                    if(Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(myFile);
                        return true;
                    }
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, myFile.getPath() + " Sorry, some thing wrong!", "Fail to open",
                            JOptionPane.ERROR_MESSAGE);
                    return true;
                }
            }
            if (myFile.isDirectory() && myFile.canRead()){
                if(searchFile(fileName, myFile)){
                    return true;
                }
            }
        }
        return false;
    }



    // Ui
    public uii() throws IOException {
        setTitle("File System Demo by 201711621428 吴伟盛");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // JFileChooser init
        String path = File.listRoots()[0].getPath();
        String rootPath = new String();
        chooser = new JFileChooser(path);
        chooser.setDialogTitle("Choose a dir for this miniFileSystem");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setPreferredSize(new Dimension(800, 600));
        int result = chooser.showOpenDialog(this);
        if (result == chooser.APPROVE_OPTION){
            System.out.println(chooser.getSelectedFile().getAbsolutePath());
            rootPath = chooser.getSelectedFile().getPath();
        }

        // help init
        JLabel help = new JLabel(helpMessage);
        help.setFont(new Font("微软雅黑", Font.CENTER_BASELINE, 20));
        JScrollPane helpPane = new JScrollPane(help);
        helpPane.setPreferredSize(new Dimension(500, 600));
        JOptionPane.showMessageDialog(null,
                helpPane,
                "文件系统模拟",
                JOptionPane.DEFAULT_OPTION);

        // Create work space
        rootFile = new File(rootPath + File.separator + "myFileSystem");
        readMe = new File(rootPath + File.separator + "myFileSystem" + File.separator + "ReadMe.txt");

        boolean flag = true;

        // JTree init
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FCB('A',rootFile,rootFile.getName(),"FileSystem",3072,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(rootFile.lastModified()))));
        if (!rootFile.exists()) {
            flag = false;
            try {
                rootFile.mkdir();
                readMe.createNewFile();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "The place is not support to create dir!", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            FileWriter writer = new FileWriter(readMe.getPath());
            writer.write("Hello, this my file system!!!\n");
            writer.write("Space: 3 * 1024K = 3072KB\n");
            writer.write("Free-Space Management:bitmap\n");
            writer.write("Store-Space Management:FAT\n");
            writer.flush();
            writer.close();
        }

        File fileC = new File(rootFile.getPath() + File.separator + "C");
        File fileD = new File(rootFile.getPath() + File.separator + "D");
        File fileE = new File(rootFile.getPath() + File.separator + "E");

        diskA = new Disk('A', rootFile, flag);
        diskC = new Disk('C', fileC, flag);
        disks.put('C',diskC);
        diskD = new Disk('D', fileD, flag);
        disks.put('D',diskD);
        diskE = new Disk('E', fileE, flag);
        disks.put('E',diskE);

        root.add(new DefaultMutableTreeNode(new FCB('C',fileC,fileC.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileC.lastModified())))));
        model.addRow(new FCB('C',fileC,fileC.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileC.lastModified()))));
        ((DefaultMutableTreeNode)root.getChildAt(0)).add(new DefaultMutableTreeNode("temp"));

        root.add(new DefaultMutableTreeNode(new FCB('D',fileD,fileD.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileD.lastModified())))));
        model.addRow(new FCB('D',fileD,fileC.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileC.lastModified()))));
        ((DefaultMutableTreeNode)root.getChildAt(1)).add(new DefaultMutableTreeNode("temp"));

        root.add(new DefaultMutableTreeNode(new FCB('E',fileE,fileE.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileE.lastModified())))));
        model.addRow(new FCB('E',fileE,fileC.getName(),"Disk",1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(fileC.lastModified()))));
        ((DefaultMutableTreeNode)root.getChildAt(2)).add(new DefaultMutableTreeNode("temp"));

        // Table init
        fileTable = new JTable(model);
        fileTable.getTableHeader().setFont(new Font(Font.DIALOG,Font.CENTER_BASELINE,24));
        fileTable.setSelectionBackground(Color.ORANGE);

        fileTable.updateUI();

        final DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setEditable(false);
        tree.putClientProperty("Jtree.lineStyle",  "Horizontal");
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB fcb = (FCB)node.getUserObject();

                model.removeRows(0, model.getRowCount());
                model.addRow(fcb);
                fileTable.updateUI();
            }
        });
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode parent = null;
                TreePath parentPath = event.getPath();
                if (parentPath == null){
                    parent = root;
                }else{
                    parent = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
                }

                char blokName = ((FCB)parent.getUserObject()).getDiskName();

                File rootFile = new File(((FCB)parent.getUserObject()).getFilePath());
                File [] childFiles = rootFile.listFiles();

                model.removeRows(0, model.getRowCount());
                for (File myFile : childFiles){
                    DefaultMutableTreeNode node = null;
                    node = new DefaultMutableTreeNode(new FCB(blokName,myFile,myFile.getName(),"File",myFile.length()/1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(myFile.lastModified()))));

                    if (myFile.isDirectory() && myFile.canRead()) {
                        node.add(new DefaultMutableTreeNode("temp"));
                    }

                    treeModel.insertNodeInto(node, parent,parent.getChildCount());
                    model.addRow(new FCB(blokName,myFile, myFile.getName(),"File",myFile.length()/1024, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(myFile.lastModified()))));
                }
                if (parent.getChildAt(0).toString().equals("temp") && parent.getChildCount() != 1)
                    treeModel.removeNodeFromParent((MutableTreeNode) parent.getChildAt(0));
                fileTable.updateUI();
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode parent = null;
                TreePath parentPath = event.getPath();
                if (parentPath == null){
                    parent = root;
                }else{
                    parent = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
                }
                if (!(parent.getChildCount() == 1 && parent.getChildAt(0).toString().equals("temp"))) {
                    if (parent.getChildCount() > 0) {
                        int count = parent.getChildCount();
                        for (int i = count - 1; i >= 0; i--) {
                            DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) parent.getChildAt(i);
                            FCB fcb = (FCB) dmt.getUserObject();
                            Disk currentDisk = disks.get(fcb.getDiskName());
                            File file = new File(fcb.getFilePath());
                            if (!(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())).equals(fcb.getModifiedTime()))) {
                                if (file.isFile()) {
                                    try {
                                        currentDisk.modifyFile(file, fcb.getSpace());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            treeModel.removeNodeFromParent((MutableTreeNode) parent.getChildAt(i));
                        }
                        treeModel.insertNodeInto(new DefaultMutableTreeNode("temp"), parent, parent.getChildCount());
                    }
//                }else treeModel.removeNodeFromParent((MutableTreeNode) parent.getChildAt(0));
                }
                model.removeRows(0, model.getRowCount());
                fileTable.updateUI();
            }
        });
        treePane = new JScrollPane(tree);
        treePane.setPreferredSize(new Dimension(150, 400));
        add(treePane, BorderLayout.WEST);

        tablePane = new JScrollPane(fileTable);
        add(tablePane, BorderLayout.CENTER);


        // Mouse DoubleClick to open a file
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1){
                    String fileName = ((String) model.getValueAt(fileTable.getSelectedRow(), 0));
                    String filePath = ((String) model.getValueAt(fileTable.getSelectedRow(), 1));
                    try {
                        if(Desktop.isDesktopSupported()) {
                            Desktop desktop = Desktop.getDesktop();
                            desktop.open(new File(filePath));
                        }
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, "Sorry, some thing wrong!", "Fail to open",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    JOptionPane.showMessageDialog(null, "File Name: " + fileName + "\n File Path: " + filePath, "content",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // Menu init
        final JPopupMenu myMenu = new JPopupMenu();
        myMenu.setPreferredSize(new Dimension(300, 200));

        //copy file or dir
        JMenuItem copyItem = new JMenuItem("copy");
        copyItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                char blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);
                File file = new File(temp.getFilePath());
                if (file.getName().equals("C") || file.getName().equals("D") || file.getName().equals("E") || file.getName().equals("CBitMap&&Fat.txt")
                        || file.getName().equals("DBitMap&&Fat.txt") || file.getName().equals("EBitMap&&Fat.txt")
                        || file.getName().equals("recover.txt")){
                    JOptionPane.showMessageDialog(null, "The dir couldn't be copy!", "Copy fail", JOptionPane.ERROR_MESSAGE);
                }
                sourceFile = file;
			}
        });
        myMenu.add(copyItem);

        //paste file or dir
        JMenuItem pasteItem = new JMenuItem("paste");
        pasteItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                char blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);
                if (currentDisk.pasteFile(sourceFile,new File(temp.getFilePath()))){
                    JOptionPane.showMessageDialog(null, "Paste success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);
                }
			}
        	
        });
        myMenu.add(pasteItem);
        
        // Create a file and update fileTable to show it
        JMenuItem createFileItem = new JMenuItem("create a file");
        createFileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                char blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);

                String inputValue;
                double capacity = 0;

                JOptionPane inputPane = new JOptionPane();
                inputPane.setPreferredSize(new Dimension(600, 600));
                inputPane.setInputValue(JOptionPane.showInputDialog("File name:"));
                if (inputPane.getInputValue() == null) {
                    return;
                }
                inputValue = inputPane.getInputValue().toString();
                File newFile = new File(temp.getFilePath() + File.separator + inputValue + ".txt");
                if (!newFile.exists()) {
					try {
						newFile.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
                if(Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.open(newFile);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                FCB fcb = new FCB(blokName,newFile,newFile.getName(),"File",newFile.length()/1024,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(newFile.lastModified())));
//                if (!newFile.exists() && !inputValue.equals(null)){
                    try {
                        if (currentDisk.createFile(newFile, capacity)) {
                            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(fcb);
                            node.add(newNode);
                            tree.updateUI();
                            model.removeRows(0, model.getRowCount());
                            model.addRow(fcb);
                            fileTable.updateUI();
                            upDateDisk(currentDisk);
                            JOptionPane.showMessageDialog(null, "Create success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);


                        }
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, "Create fail!!!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
//
        });
        myMenu.add(createFileItem);

        // create a dir and update fileTable to show it
        JMenuItem createDirItem = new JMenuItem("create a dir");
        createDirItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                char blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);
                String inputValue = JOptionPane.showInputDialog("Dir name:");
                if (inputValue == null) {
                    return;
                }
                File newDir = new File(temp.getFilePath() + File.separator + inputValue);
                if (newDir.exists())
                    deleteDirectory(newDir.getPath());
                try{
                    newDir.mkdir();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new FCB(blokName,newDir,inputValue, "Directory",0, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(newDir.lastModified()))));
                    newNode.add(new DefaultMutableTreeNode("temp"));
                    model.removeRows(0, model.getRowCount());
                    model.addRow(new FCB(blokName,newDir,inputValue, "Directory",0, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(newDir.lastModified()))));
                    fileTable.updateUI();
                    upDateDisk(currentDisk);
                    JOptionPane.showMessageDialog(null, "Create success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);
                }catch (Exception E){
                    JOptionPane.showMessageDialog(null, "Create fail!!!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        myMenu.add(createDirItem);

        // Delete a file or a dir
        JMenuItem deleteItem = new JMenuItem("delete");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                char blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);
                int choose = JOptionPane.showConfirmDialog(null, "Are you sure to delete this file/dir?", "confirm", JOptionPane.YES_NO_OPTION);
                if (choose == 0){
                    if (currentDisk.deleteFile(temp.getMyFile(), temp.getSpace())){
                        try {
                            currentDisk.rewriteBitMap();
                            currentDisk.rewriteRecoverWriter();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        upDateDisk(currentDisk);
                        JOptionPane.showMessageDialog(null, "Delete success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);
                    }else{
                        JOptionPane.showMessageDialog(null, "Delete fail!!!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        myMenu.add(deleteItem);

        // Format a dir
        JMenuItem formatItem = new JMenuItem("format");
        formatItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                int blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);
                int choose = JOptionPane.showConfirmDialog(null, "Are you sure to format this dir?", "confirm", JOptionPane.YES_NO_OPTION);
                if (choose == 0){
                    try{
                    if (temp.getMyFile().isDirectory()) {
                        for (File myfile : temp.getMyFile().listFiles()) {
                            currentDisk.deleteFile(myfile, getSpace(myfile));
                        }
                        upDateDisk(currentDisk);
                        JOptionPane.showMessageDialog(null, "Format success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);
                        currentDisk.rewriteBitMap();
                    }
                    }catch (Exception E1){
                        JOptionPane.showMessageDialog(null, "Format fail!!!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
//        myMenu.add(formatItem);

        // Rename a dir/file
        JMenuItem renameItem = new JMenuItem("rename");
//        renameItem.setFont(new Font("微软雅黑",Font.CENTER_BASELINE,24));
        renameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                FCB temp = (FCB)node.getUserObject();
                int blokName = temp.getDiskName();
                Disk currentDisk = disks.get(blokName);

                String inputValue = null;
                JOptionPane inputPane = new JOptionPane();
                inputPane.setInputValue(JOptionPane.showInputDialog("New file name:"));
                if (inputPane.getInputValue() == null) {
                    return;
                }
                inputValue = inputPane.getInputValue().toString();
                try {
                    currentDisk.renameFile(temp.getMyFile(), inputValue, temp.getSpace());
                    JOptionPane.showMessageDialog(null, "Rename success! Reopen the parent dir to reflash!", "Success", JOptionPane.DEFAULT_OPTION);
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "Rename fail!!!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
//        myMenu.add(renameItem);

        // Information for the disk
        JPanel panel = new JPanel();
        panel.setBackground(Color.green);
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel tips = new JLabel("文件操作:选中左侧文件之后右键 打开文件:双击右侧表格内文件");
        panel.add(tips);
        panel.add(diskName);
        nameField.setForeground(Color.RED);
        panel.add(nameField);
        panel.add(new JLabel("  "));
        panel.add(haveUsed);
        usedField.setForeground(Color.RED);
        panel.add(usedField);
        panel.add(new JLabel("  "));
        panel.add(freeYet);
        freeField.setForeground(Color.RED);
        panel.add(freeField);
        panel.add(new JLabel("  "));
        panel.add(fileNum);
        fileNumField.setForeground(Color.RED);
        panel.add(fileNumField);
        add(panel, BorderLayout.SOUTH);


        // SeachLine init
        JPanel searchPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JLabel searchLabel = new JLabel("Search(eg. File:a.txt Dir:a): ");
        searchPane.add(searchLabel);
        searchLine.setPreferredSize(new Dimension(500, 50));
        searchPane.add(searchLine);
        JButton searchButton = new JButton("start");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileName = searchLine.getText();
                if(!searchFile(fileName, rootFile)){
                    JOptionPane.showMessageDialog(null, "Can not find this file!", "Fail!", JOptionPane.WARNING_MESSAGE);
                }
                searchLine.setText("");
            }
        });
        searchPane.add(searchButton);
        add(searchPane, BorderLayout.NORTH);

        // Listen to the tree
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON3){
                    myMenu.show(e.getComponent(), e.getX(), e.getY());

                }
            }
        });

        setSize(1200, 600);
        setVisible(true);
    }

    public static void main(String args[]) throws IOException {
        new uii();
    }
}

