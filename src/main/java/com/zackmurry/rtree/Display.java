package com.zackmurry.rtree;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.ConcurrentModificationException;

public class Display extends Canvas implements Runnable {

    public static int WIDTH = 800;
    public static int HEIGHT = 600;

    private Thread thread;
    private static final String title = "R-Tree Visualizer";
    private static final Font font = new Font("Serif", Font.PLAIN, 24);

    private JFrame frame;

    private static boolean running = false;

    private final RTree<Integer> rtree = new RTree<>();

    class ClickDetector implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            System.out.println("inserting at (" + mouseEvent.getX() + ", " + mouseEvent.getY() + ")");
            rtree.insert(new Rectangle(mouseEvent.getX(), mouseEvent.getY(), 10, 20), 0);
            System.out.println(rtree);
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {

        }
    }

    public Display() {
        this.frame = new JFrame();
        Dimension size = new Dimension(WIDTH, HEIGHT);
        this.setPreferredSize(size);
        addMouseListener(new ClickDetector());
    }

    public synchronized void start() {
        running = true;
        this.thread = new Thread(this, "Display");
        this.thread.start();
    }

    public synchronized void stop(){
        running = false;
        try{
            this.thread.join();
        } catch (Exception e){
            e.printStackTrace();
            this.thread.interrupt();
        }
    }

    @Override
    public void run() {
        try{
            while(running) {
                render();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        g.setFont(font);

        g.setColor(Color.WHITE);
        g.fillRect(0,0,WIDTH,HEIGHT);

        // Draw R-Tree
        draw(g);

        g.dispose();
        bs.show();
    }

    public void draw(Graphics g) {
        g.setColor(new Color(255, 255, 255));
        g.fillRect(-WIDTH, -HEIGHT, 2*WIDTH, 2*HEIGHT);

        drawNode(rtree, g, 0);
    }

    private void drawNode(RTree<Integer> node, Graphics g, int depth) {
        if (!node.children.isEmpty() && !node.entries.isEmpty()) {
            System.out.println("Node with children and entries!!!");
        }
        try {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(Math.min(depth * 50, 255), 0, 0));
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(node.bounds.x, node.bounds.y, node.bounds.width, node.bounds.height);
            g.setColor(new Color(30, 94, 232));
            for (RTreeEntry<Integer> entry : node.entries) {
                g.fillRect(entry.bounds.x, entry.bounds.y, entry.bounds.width, entry.bounds.height);
            }
            for (RTree<Integer> child : node.children) {
                drawNode(child, g, depth + 1);
            }
        } catch (ConcurrentModificationException e) {
            drawNode(node, g, depth);
        }
    }

    public static void main(String[] args) {
        Display display = new Display();
        display.frame.setTitle(title);

        display.frame.setLayout(new GridBagLayout());

        JPanel mainPanel = new JPanel();
//        JPanel sidePanel = new JPanel();

//        sidePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 25));

        mainPanel.add(display);
        display.frame.add(mainPanel);

//        sidePanel.setPreferredSize(new Dimension(200, Display.HEIGHT));

//        display.frame.add(sidePanel);

        SwingUtilities.updateComponentTreeUI(display.frame);

        display.frame.pack();
        display.frame.getContentPane().setBackground(Color.WHITE);
        display.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        display.frame.setLocationRelativeTo(null);
        display.frame.setBackground(Color.BLACK);
        display.frame.setResizable(false);
        display.frame.setVisible(true);

        display.start();
    }

}
