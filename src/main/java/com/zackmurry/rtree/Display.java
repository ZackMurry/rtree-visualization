package com.zackmurry.rtree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.util.ConcurrentModificationException;
import java.util.List;

public class Display extends Canvas implements Runnable {

    public static int WIDTH = 800;
    public static int HEIGHT = 600;

    private Thread thread;
    private static final String title = "R-Tree Visualizer";
    private static final Font font = new Font("Serif", Font.PLAIN, 24);

    private JFrame frame;

    private static boolean running = false;

    private final RTree<Rectangle> rtree = new RTree<>();
    private Point dragStart = null;
    private Rectangle searchRect = null;
    private List<Rectangle> searchedRects = null;

    class ClickDetector implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                var rect = new Rectangle(mouseEvent.getX(), mouseEvent.getY(), 10, 20);
                rtree.insert(rect, rect);
                System.out.println(rtree);
            } else if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                searchRect = null;
                dragStart = null;
                searchedRects = null;
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                dragStart = new Point(mouseEvent.getX(), mouseEvent.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                int x1 = Math.min(mouseEvent.getX(), dragStart.x);
                int y1 = Math.min(mouseEvent.getY(), dragStart.y);
                int x2 = Math.max(mouseEvent.getX(), dragStart.x);
                int y2 = Math.max(mouseEvent.getY(), dragStart.y);
                searchRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
                searchedRects = rtree.search(searchRect);
                dragStart = null;
            }
        }

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {

        }
    }

    class DragDetector implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            if (dragStart != null) {
                int x1 = Math.min(mouseEvent.getX(), dragStart.x);
                int y1 = Math.min(mouseEvent.getY(), dragStart.y);
                int x2 = Math.max(mouseEvent.getX(), dragStart.x);
                int y2 = Math.max(mouseEvent.getY(), dragStart.y);
                searchRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
                searchedRects = rtree.search(searchRect);
            }
        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {

        }
    }

    public Display() {
        this.frame = new JFrame();
        Dimension size = new Dimension(WIDTH, HEIGHT);
        this.setPreferredSize(size);
        addMouseListener(new ClickDetector());
        addMouseMotionListener(new DragDetector());
    }

    public synchronized void start() {
        running = true;
        this.thread = new Thread(this, "Display");
        this.thread.start();
    }

    public synchronized void stop() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }
        Toolkit.getDefaultToolkit().sync();

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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(new Color(50, 204, 89));
        g2d.setStroke(new BasicStroke(5));
        if (searchedRects != null) {
            for (Rectangle rect : searchedRects) {
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
        g2d.setStroke(new BasicStroke(3));
        if (searchRect != null) {
            g2d.drawRect(searchRect.x, searchRect.y, searchRect.width, searchRect.height);
        }
    }

    private void drawNode(RTree<Rectangle> node, Graphics g, int depth) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(Math.min(depth * 50, 255), 0, 0));
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(node.bounds.x, node.bounds.y, node.bounds.width, node.bounds.height);
            g.setColor(new Color(30, 94, 232));
            for (RTreeEntry<Rectangle> entry : node.entries) {
                g.fillRect(entry.bounds.x, entry.bounds.y, entry.bounds.width, entry.bounds.height);
            }
            for (RTree<Rectangle> child : node.children) {
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
        mainPanel.add(display);
        display.frame.add(mainPanel);
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
