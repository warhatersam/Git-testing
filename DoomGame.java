import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;

/** A self-contained, software-rendered corridor shooter. No external assets. */
public class DoomGame extends JPanel implements ActionListener {
    static final int W = 640, H = 400;
    static final String[] MAP = {
        "1111111111111111",
        "1000000100000001",
        "1000000100000001",
        "1000000000000001",
        "1001100100110001",
        "1000000100000001",
        "1000000000000001",
        "1110111111101101",
        "1000000000000001",
        "1011100111000001",
        "1000000100000001",
        "1000000100111001",
        "1000000000000001",
        "1001100100000001",
        "1000000100000001",
        "1111111111111111"
    };
    final boolean[] keys = new boolean[600];
    final BufferedImage frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    final double[] depth = new double[W];
    final ArrayList<Enemy> enemies = new ArrayList<>();
    double px, py, angle, cooldown, flash, hurt, elapsed;
    int health, kills;
    boolean started, won;
    long lastTime = System.nanoTime();
    final javax.swing.Timer timer;

    static class Enemy {
        double x, y, attack;
        int hp = 3;
        Enemy(double x, double y) { this.x = x; this.y = y; }
    }

    public DoomGame() {
        setPreferredSize(new Dimension(960, 600));
        setFocusable(true);
        reset();
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = true;
                if (e.getKeyCode() == KeyEvent.VK_ENTER) started = true;
                if (e.getKeyCode() == KeyEvent.VK_R && (health <= 0 || won)) {
                    reset(); started = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    started = false;
                    java.util.Arrays.fill(keys, false);
                }
            }
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false;
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(e)) keys[KeyEvent.VK_SPACE] = true;
            }
            public void mouseReleased(MouseEvent e) { keys[KeyEvent.VK_SPACE] = false; }
        });
        addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                java.util.Arrays.fill(keys, false);
                started = false;
            }
        });
        timer = new javax.swing.Timer(16, this);
    }

    void reset() {
        px = 2.5; py = 2.5; angle = 0;
        health = 100; kills = 0; won = false;
        cooldown = flash = hurt = elapsed = 0;
        enemies.clear();
        double[][] spawns = {{5.5,3.5},{10.5,2.5},{13.5,5.5},{4.5,6.5},
            {3.5,10.5},{8.5,8.5},{13.5,10.5},{5.5,13.5},{10.5,14.5},{13.5,13.5}};
        for (double[] s : spawns) enemies.add(new Enemy(s[0], s[1]));
    }

    boolean wall(double x, double y) {
        return x < 0 || y < 0 || y >= MAP.length || x >= MAP[0].length()
            || MAP[(int)y].charAt((int)x) == '1';
    }

    boolean clear(double x, double y, double radius) {
        return !wall(x-radius,y-radius) && !wall(x+radius,y-radius)
            && !wall(x-radius,y+radius) && !wall(x+radius,y+radius);
    }

    boolean visible(double x, double y) {
        double distance = Math.hypot(x-px, y-py);
        int steps = Math.max(1, (int)(distance / .035));
        for (int i = 1; i <= steps; i++) {
            double t = (double)i/steps;
            if (wall(px+(x-px)*t, py+(y-py)*t)) return false;
        }
        return true;
    }

    void shoot() {
        if (cooldown > 0) return;
        cooldown = .24; flash = .09;
        Enemy target = null;
        double nearest = 30;
        for (Enemy e : enemies) {
            if (e.hp <= 0) continue;
            double dx = e.x-px, dy = e.y-py;
            double forward = dx*Math.cos(angle)+dy*Math.sin(angle);
            double side = -dx*Math.sin(angle)+dy*Math.cos(angle);
            if (forward > 0 && Math.abs(side) < .30 && forward < nearest && visible(e.x,e.y)) {
                nearest = forward; target = e;
            }
        }
        if (target != null && --target.hp == 0) kills++;
        won = kills == enemies.size();
    }

    void update(double dt) {
        if (!started || health <= 0 || won) return;
        elapsed += dt;
        cooldown = Math.max(0,cooldown-dt);
        flash = Math.max(0,flash-dt); hurt = Math.max(0,hurt-dt);
        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_Q]) angle -= dt*2.1;
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_E]) angle += dt*2.1;
        double forward = (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP] ? 1 : 0)
            - (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN] ? 1 : 0);
        double side = (keys[KeyEvent.VK_D] ? 1 : 0)-(keys[KeyEvent.VK_A] ? 1 : 0);
        double speed = (keys[KeyEvent.VK_SHIFT] ? 3.8 : 2.5)*dt / Math.max(1,Math.hypot(forward,side));
        double dx = (Math.cos(angle)*forward-Math.sin(angle)*side)*speed;
        double dy = (Math.sin(angle)*forward+Math.cos(angle)*side)*speed;
        if (clear(px+dx,py,.20)) px += dx;
        if (clear(px,py+dy,.20)) py += dy;
        if (keys[KeyEvent.VK_SPACE]) shoot();
        for (Enemy e : enemies) {
            if (e.hp <= 0) continue;
            e.attack -= dt;
            double distance = Math.hypot(px-e.x,py-e.y);
            if (distance < 9 && visible(e.x,e.y)) {
                if (distance > .75) {
                    double ex = (px-e.x)/distance*dt*.85;
                    double ey = (py-e.y)/distance*dt*.85;
                    if (clear(e.x+ex,e.y,.23)) e.x += ex;
                    if (clear(e.x,e.y+ey,.23)) e.y += ey;
                } else if (e.attack <= 0) {
                    health = Math.max(0,health-9); hurt = .22; e.attack = .85;
                }
            }
        }
    }

    void renderWorld(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            int shade = y < H/2 ? 15+(H/2-y)/10 : 25+(y-H/2)/5;
            g.setColor(new Color(shade,shade, Math.max(0,shade-3)));
            g.drawLine(0,y,W,y);
        }
        double dirX = Math.cos(angle), dirY = Math.sin(angle);
        for (int x = 0; x < W; x++) {
            double camera = (2.0*x/W-1)*.66;
            double rayX = dirX-dirY*camera, rayY = dirY+dirX*camera;
            int mx = (int)px, my = (int)py;
            double deltaX = Math.abs(1/rayX), deltaY = Math.abs(1/rayY);
            int stepX = rayX < 0 ? -1 : 1, stepY = rayY < 0 ? -1 : 1;
            double sideX = (rayX < 0 ? px-mx : mx+1-px)*deltaX;
            double sideY = (rayY < 0 ? py-my : my+1-py)*deltaY;
            boolean ySide = false;
            for (int n = 0; n < 64; n++) {
                if (sideX < sideY) { sideX += deltaX; mx += stepX; ySide = false; }
                else { sideY += deltaY; my += stepY; ySide = true; }
                if (wall(mx+.5,my+.5)) break;
            }
            double distance = Math.max(.01,ySide ? sideY-deltaY : sideX-deltaX);
            depth[x] = distance;
            int height = (int)(H/distance);
            double hit = ySide ? px+distance*rayX : py+distance*rayY;
            hit -= Math.floor(hit);
            for (int y = Math.max(0,H/2-height/2); y < Math.min(H,H/2+height/2); y++) {
                double v = (double)(y-(H/2-height/2))/height;
                int row = (int)(v*8);
                boolean seam = (v*8)%1 < .065 || ((hit*4+(row%2)*.5)%1) < .035;
                double light = Math.max(.20,1/(1+distance*.13))*(ySide ? .72 : 1);
                int r = seam ? 35 : 112, green = seam ? 32 : 103, b = seam ? 30 : 83;
                if ((mx+my)%5 == 0) { r += 30; green -= 18; }
                frame.setRGB(x,y,((int)(r*light)<<16)|((int)(green*light)<<8)|(int)(b*light));
            }
        }
        ArrayList<Enemy> sorted = new ArrayList<>(enemies);
        sorted.sort(Comparator.comparingDouble((Enemy e) -> -Math.hypot(e.x-px,e.y-py)));
        for (Enemy e : sorted) {
            if (e.hp <= 0) continue;
            double dx = e.x-px, dy = e.y-py;
            double z = dx*dirX+dy*dirY;
            if (z < .15) continue;
            double lateral = -dx*dirY+dy*dirX;
            int size = Math.min(3000,(int)(H/z));
            int center = (int)(W/2.0*(1+lateral/(z*.66)));
            int top = H/2-size/2;
            for (int x = Math.max(0,center-size/3); x < Math.min(W,center+size/3); x++) {
                if (z >= depth[x]) continue;
                double u = (double)(x-center)/size;
                for (int y = Math.max(0,top); y < Math.min(H,top+size); y++) {
                    double v = (double)(y-top)/size;
                    int color = 0;
                    if (v > .08 && v < .34 && Math.abs(u) < .14) color = 0xb26748;
                    if (v >= .34 && v < .72 && Math.abs(u) < .24) color = 0x8e2927;
                    if (v >= .72 && Math.abs(u) > .035 && Math.abs(u) < .19) color = 0x403835;
                    if (v > .19 && v < .23 && Math.abs(u) > .035 && Math.abs(u) < .11) color = 0xffdb63;
                    if (v > .04 && v < .16 && Math.abs(u) > .10 && Math.abs(u) < .18) color = 0xbfb391;
                    if (color != 0) {
                        double light = Math.max(.30,1/(1+z*.10));
                        int r = (int)(((color>>16)&255)*light);
                        int green = (int)(((color>>8)&255)*light), b = (int)((color&255)*light);
                        frame.setRGB(x,y,(r<<16)|(green<<8)|b);
                    }
                }
            }
        }
    }

    void render() {
        Graphics2D g = frame.createGraphics();
        renderWorld(g);
        int bob = started && (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_A] || keys[KeyEvent.VK_D] || keys[KeyEvent.VK_S])
            ? (int)(Math.sin(elapsed*12)*4) : 0;
        int recoil = flash > 0 ? 12 : 0;
        g.setColor(new Color(105,70,49));
        g.fillPolygon(new int[]{350,389,439,394},new int[]{340+bob,329+bob,400,400},4);
        g.setColor(new Color(31,33,36));
        g.fillPolygon(new int[]{294,333,370,288},new int[]{287+bob+recoil,287+bob+recoil,400,400},4);
        g.setColor(new Color(102,108,110)); g.fillRect(302,285+bob+recoil,24,76);
        g.setColor(new Color(43,47,48)); g.fillRect(309,292+bob+recoil,10,63);
        if (flash > 0) {
            g.setColor(new Color(255,190,42));
            g.fillPolygon(new int[]{312,292,304,279,306,316,323,347,328,339,321},
                new int[]{288,260,262,235,246,218,247,237,268,276,278},11);
            g.setColor(new Color(255,246,176)); g.fillOval(304,250,20,34);
        }
        g.setColor(new Color(220,222,192));
        g.drawLine(W/2-7,H/2,W/2-3,H/2); g.drawLine(W/2+3,H/2,W/2+7,H/2);
        g.drawLine(W/2,H/2-7,W/2,H/2-3); g.drawLine(W/2,H/2+3,W/2,H/2+7);
        if (hurt > 0) { g.setColor(new Color(200,0,0,70)); g.fillRect(0,0,W,H); }
        g.setColor(new Color(13,16,19)); g.fillRect(0,H-38,W,38);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));
        g.setColor(health < 30 ? new Color(255,80,65) : new Color(224,216,178));
        g.drawString("HEALTH "+health,16,H-14);
        g.setColor(new Color(224,216,178));
        g.drawString("AMMO INF",236,H-14); g.drawString("KILLS "+kills+" / "+enemies.size(),457,H-14);
        // Always-visible tactical map helps the player find remaining enemies.
        int scale = 5, ox = W-90, oy = 10;
        g.setColor(new Color(0,0,0,180)); g.fillRect(ox-3,oy-3,86,86);
        for (int y=0;y<MAP.length;y++) for(int x=0;x<MAP[y].length();x++) {
            if (MAP[y].charAt(x)=='1') { g.setColor(new Color(95,92,78)); g.fillRect(ox+x*scale,oy+y*scale,scale,scale); }
        }
        g.setColor(new Color(255,80,60));
        for (Enemy e:enemies) if(e.hp>0) g.fillRect(ox+(int)(e.x*scale)-1,oy+(int)(e.y*scale)-1,3,3);
        g.setColor(new Color(112,235,194));
        int mapX=ox+(int)(px*scale), mapY=oy+(int)(py*scale);
        g.fillOval(mapX-2,mapY-2,4,4);
        g.drawLine(mapX,mapY,mapX+(int)(Math.cos(angle)*7),mapY+(int)(Math.sin(angle)*7));
        if (!started || health<=0 || won) {
            g.setColor(new Color(5,8,11,225)); g.fillRect(0,0,W,H);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,48));
            g.setColor(new Color(244,103,66));
            centered(g,health<=0 ? "YOU DIED" : won ? "SECTOR CLEARED" : "IRON DESCENT",126);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,16));
            g.setColor(new Color(220,220,201));
            centered(g,"A DOOM-STYLE JAVA SHOOTER",164);
            centered(g,"WASD move   |   Arrow Left/Right or Q/E turn",218);
            centered(g,"Space / Left click fire   |   Shift sprint",247);
            centered(g,"Esc pause   |   Eliminate all 10 creatures",276);
            g.setColor(new Color(244,177,88));
            centered(g,health<=0 || won ? "PRESS R TO PLAY AGAIN" : "PRESS ENTER TO ENTER THE SECTOR",330);
        }
        g.dispose();
    }

    void centered(Graphics2D g,String text,int y) {
        g.drawString(text,(W-g.getFontMetrics().stringWidth(text))/2,y);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        render();
        int width = getWidth(), height = getHeight();
        double scale = Math.min((double)width/W,(double)height/H);
        int rw = (int)(W*scale), rh = (int)(H*scale);
        graphics.setColor(Color.BLACK); graphics.fillRect(0,0,width,height);
        graphics.drawImage(frame,(width-rw)/2,(height-rh)/2,rw,rh,null);
    }

    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = Math.min(.05,(now-lastTime)/1e9);
        lastTime = now; update(dt); repaint();
    }

    static void selfTest() {
        DoomGame game = new DoomGame();
        if (!game.clear(game.px,game.py,.2)) throw new AssertionError("Player spawn blocked");
        for (Enemy e:game.enemies) if(!game.clear(e.x,e.y,.23)) throw new AssertionError("Enemy spawn blocked");
        game.enemies.clear();
        Enemy target = new Enemy(4.5,2.5); game.enemies.add(target);
        for(int i=0;i<3;i++) { game.cooldown=0; game.shoot(); }
        if(game.kills!=1 || !game.won) throw new AssertionError("Shooting or victory failed");
        game.reset(); game.enemies.clear();
        Enemy blocked = new Enemy(10.5,2.5); game.enemies.add(blocked); game.shoot();
        if(blocked.hp!=3) throw new AssertionError("Shot passed through wall");
        game.reset(); game.started=true; game.keys[KeyEvent.VK_S]=true;
        for(int i=0;i<100;i++) game.update(.05);
        if(!game.clear(game.px,game.py,.2) || game.px<1.2) throw new AssertionError("Wall collision failed");
        game.keys[KeyEvent.VK_S]=false;
        game.health=100;
        game.enemies.clear(); game.enemies.add(new Enemy(game.px+.4,game.py));
        game.update(.05);
        if(game.health!=91) throw new AssertionError("Enemy attack failed");
        game.reset(); game.render(); game.started=true; game.render();
        System.out.println("PASS: spawns, shooting, victory, wall occlusion, movement collision, enemy attack, rendering");
    }

    public static void main(String[] args) {
        if (args.length>0 && args[0].equals("--self-test")) { selfTest(); return; }
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Iron Descent | Java FPS");
            DoomGame game = new DoomGame();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(game); window.pack(); window.setLocationRelativeTo(null);
            window.setVisible(true); game.requestFocusInWindow();
            game.lastTime = System.nanoTime(); game.timer.start();
        });
    }
}
