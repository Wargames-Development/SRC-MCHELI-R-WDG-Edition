package mcheli.economy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class MCH_GuiEconomyTechTree extends GuiScreen {

    private static final int BTN_ACTION = 1;
    private static final int BTN_RESET_VIEW = 2;
    private static final int BTN_CLOSE = 3;
    private static final int NODE_W = 26;
    private static final int NODE_H = 26;
    private static final int NODE_GRID_X = 86;
    private static final int NODE_GRID_Y = 58;
    private static final ResourceLocation TEX_ACHIEVEMENT_BG = new ResourceLocation("textures/gui/achievement/achievement_background.png");
    private static final ResourceLocation TEX_ICON_SL = new ResourceLocation("mcheli", "textures/gui/economy/coin_sl.png");
    private static final ResourceLocation TEX_ICON_GE = new ResourceLocation("mcheli", "textures/gui/economy/coin_ge.png");
    private static final ResourceLocation TEX_ICON_RP = new ResourceLocation("mcheli", "textures/gui/economy/coin_rp.png");

    private final EntityPlayer player;
    private final List<MCH_EconomyTechNode> nodes = new ArrayList<MCH_EconomyTechNode>();
    private final Map<String, Point> nodePos = new LinkedHashMap<String, Point>();
    private final Map<String, ItemStack> nodeIconCache = new HashMap<String, ItemStack>();
    private final Map<String, Integer> depthCache = new HashMap<String, Integer>();
    private final Set<String> resolving = new HashSet<String>();
    private final RenderItem itemRenderer = new RenderItem();

    private String selectedNodeId = "";
    private boolean draggingTree;
    private int lastMouseX;
    private int lastMouseY;
    private int panX;
    private int panY;

    private int treeLeft;
    private int treeTop;
    private int treeWidth;
    private int treeHeight;
    private int detailLeft;
    private int detailTop;
    private int detailWidth;
    private int detailHeight;

    public MCH_GuiEconomyTechTree(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.panX = 0;
        this.panY = 0;
        this.draggingTree = false;
        this.refreshLayout();
        this.buttonList.add(new GuiButton(BTN_ACTION, this.detailLeft + 8, this.detailTop + this.detailHeight - 56, this.detailWidth - 16, 20, "执行操作"));
        this.buttonList.add(new GuiButton(BTN_RESET_VIEW, this.detailLeft + 8, this.detailTop + this.detailHeight - 32, this.detailWidth - 84, 20, "重置视图"));
        this.buttonList.add(new GuiButton(BTN_CLOSE, this.detailLeft + this.detailWidth - 72, this.detailTop + this.detailHeight - 32, 64, 20, "关闭"));
    }

    private void refreshLayout() {
        this.treeLeft = 8;
        this.treeTop = 24;
        this.detailWidth = Math.min(238, this.width / 3);
        this.detailLeft = this.width - this.detailWidth - 8;
        this.treeWidth = Math.max(180, this.detailLeft - this.treeLeft - 8);
        this.treeHeight = this.height - this.treeTop - 10;
        this.detailTop = this.treeTop;
        this.detailHeight = this.treeHeight;

        this.nodes.clear();
        Collection<MCH_EconomyTechNode> allNodes = MCH_EconomyTechRegistry.getNodes();
        this.nodes.addAll(allNodes);
        Collections.sort(this.nodes, new Comparator<MCH_EconomyTechNode>() {
            @Override
            public int compare(MCH_EconomyTechNode o1, MCH_EconomyTechNode o2) {
                int d1 = resolveDepth(o1.id);
                int d2 = resolveDepth(o2.id);
                if (d1 != d2) {
                    return d1 - d2;
                }
                return o1.id.compareTo(o2.id);
            }
        });
        this.nodePos.clear();
        this.depthCache.clear();
        this.resolving.clear();
        this.nodeIconCache.clear();
        Map<Integer, Integer> rowByDepth = new HashMap<Integer, Integer>();
        for (MCH_EconomyTechNode node : this.nodes) {
            int depth = resolveDepth(node.id);
            Integer rowObj = rowByDepth.get(Integer.valueOf(depth));
            int row = rowObj == null ? 0 : rowObj.intValue();
            rowByDepth.put(Integer.valueOf(depth), Integer.valueOf(row + 1));
            int x = depth * NODE_GRID_X;
            int y = row * NODE_GRID_Y;
            this.nodePos.put(node.id, new Point(x, y));
        }
        if (this.selectedNodeId.isEmpty() && !this.nodes.isEmpty()) {
            this.selectedNodeId = this.nodes.get(0).id;
        }
    }

    private int resolveDepth(String nodeId) {
        String normalized = MCH_EconomyTechRegistry.normalizeId(nodeId);
        Integer cache = this.depthCache.get(normalized);
        if (cache != null) {
            return cache.intValue();
        }
        if (this.resolving.contains(normalized)) {
            return 0;
        }
        this.resolving.add(normalized);
        MCH_EconomyTechNode node = MCH_EconomyTechRegistry.getNode(normalized);
        int depth = 0;
        if (node != null) {
            for (String pre : node.prerequisites) {
                depth = Math.max(depth, resolveDepth(pre) + 1);
            }
        }
        this.resolving.remove(normalized);
        this.depthCache.put(normalized, Integer.valueOf(depth));
        return depth;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) {
            return;
        }
        if (button.id == BTN_CLOSE) {
            this.mc.thePlayer.closeScreen();
            return;
        }
        if (button.id == BTN_RESET_VIEW) {
            this.panX = 0;
            this.panY = 0;
            return;
        }
        if (button.id == BTN_ACTION) {
            MCH_EconomyTechNode node = getSelectedNode();
            if (node == null) {
                return;
            }
            if (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK) {
                MCH_PacketIndEconomyTechAction.send(MCH_PacketIndEconomyTechAction.ACTION_UNLOCK_RP, node.id);
            } else if (node.type == MCH_EconomyTechNode.NodeType.SL_PURCHASE) {
                MCH_PacketIndEconomyTechAction.send(MCH_PacketIndEconomyTechAction.ACTION_PURCHASE_SL, node.id);
            } else {
                MCH_PacketIndEconomyTechAction.send(MCH_PacketIndEconomyTechAction.ACTION_EXCHANGE_GE, node.id);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        String hitNode = findNodeAt(mouseX, mouseY);
        if (!hitNode.isEmpty()) {
            this.selectedNodeId = hitNode;
            return;
        }
        if (insideTree(mouseX, mouseY)) {
            this.draggingTree = true;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long dragTime) {
        super.mouseClickMove(mouseX, mouseY, button, dragTime);
        if (button != 0 || !this.draggingTree) {
            return;
        }
        int dx = mouseX - this.lastMouseX;
        int dy = mouseY - this.lastMouseY;
        this.panX += dx;
        this.panY += dy;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button == 0) {
            this.draggingTree = false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawAchievementLikeTreeBackground();
        drawDetailPanelFrame();
        this.fontRendererObj.drawStringWithShadow("科技树 (拖拽浏览)", this.treeLeft + 6, this.treeTop + 5, 0xE0E0E0);
        drawConnections();
        drawNodes(mouseX, mouseY);
        drawDetailPanel();
        drawNodeTooltip(mouseX, mouseY);
        updateButtons();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawAchievementLikeTreeBackground() {
        drawRect(this.treeLeft - 1, this.treeTop - 1, this.treeLeft + this.treeWidth + 1, this.treeTop + this.treeHeight + 1, 0xD0000000);
        drawRect(this.treeLeft, this.treeTop, this.treeLeft + this.treeWidth, this.treeTop + this.treeHeight, 0xCC000000);
        drawRect(this.treeLeft, this.treeTop, this.treeLeft + this.treeWidth, this.treeTop + 18, 0xC0202020);
        this.mc.getTextureManager().bindTexture(TEX_ACHIEVEMENT_BG);
        int baseX = this.treeLeft + this.treeWidth / 2 + this.panX;
        int baseY = this.treeTop + 24 + this.panY;
        int startX = this.treeLeft - ((baseX - this.treeLeft) & 15);
        int startY = this.treeTop - ((baseY - this.treeTop) & 15);
        for (int y = startY; y < this.treeTop + this.treeHeight; y += 16) {
            for (int x = startX; x < this.treeLeft + this.treeWidth; x += 16) {
                this.drawTexturedModalRect(x, y, 0, 0, 16, 16);
            }
        }
        drawRect(this.treeLeft, this.treeTop, this.treeLeft + this.treeWidth, this.treeTop + this.treeHeight, 0x60301010);
    }

    private void drawDetailPanelFrame() {
        drawRect(this.detailLeft - 1, this.detailTop - 1, this.detailLeft + this.detailWidth + 1, this.detailTop + this.detailHeight + 1, 0xD0000000);
        drawRect(this.detailLeft, this.detailTop, this.detailLeft + this.detailWidth, this.detailTop + this.detailHeight, 0xC0181818);
        drawRect(this.detailLeft, this.detailTop, this.detailLeft + this.detailWidth, this.detailTop + 18, 0xC0242424);
        this.fontRendererObj.drawStringWithShadow("节点详情", this.detailLeft + 8, this.detailTop + 5, 0xEAEAEA);
    }

    private void drawConnections() {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        float pulse = (float) ((Math.sin((double) (System.currentTimeMillis() % 2000L) / 2000.0D * 6.283185307179586D) + 1.0D) * 0.5D);
        for (MCH_EconomyTechNode node : this.nodes) {
            Point dst = this.nodePos.get(node.id);
            if (dst == null) {
                continue;
            }
            int x2 = nodeScreenX(dst.x) + NODE_W / 2;
            int y2 = nodeScreenY(dst.y) + NODE_H / 2;
            for (String pre : node.prerequisites) {
                Point src = this.nodePos.get(MCH_EconomyTechRegistry.normalizeId(pre));
                if (src == null) {
                    continue;
                }
                int x1 = nodeScreenX(src.x) + NODE_W / 2;
                int y1 = nodeScreenY(src.y) + NODE_H / 2;
                drawLine(t, x1, y1, x2, y2, 0x70505050);
                if (MCH_EconomyClientData.isUnlocked(pre)) {
                    int alpha = 120 + (int) (80.0F * pulse);
                    drawLine(t, x1, y1, x2, y2, (alpha << 24) | 0xA0FF90);
                }
            }
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void drawLine(Tessellator t, int x1, int y1, int x2, int y2, int color) {
        t.startDrawing(1);
        t.setColorRGBA((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255);
        t.addVertex((double) x1, (double) y1, 0.0D);
        t.addVertex((double) x2, (double) y2, 0.0D);
        t.draw();
    }

    private void drawNodes(int mouseX, int mouseY) {
        String hoverNode = findNodeAt(mouseX, mouseY);
        this.mc.getTextureManager().bindTexture(TEX_ACHIEVEMENT_BG);
        RenderHelper.enableGUIStandardItemLighting();
        for (MCH_EconomyTechNode node : this.nodes) {
            Point p = this.nodePos.get(node.id);
            if (p == null) {
                continue;
            }
            int sx = nodeScreenX(p.x);
            int sy = nodeScreenY(p.y);
            if (!insideTree(sx + NODE_W / 2, sy + NODE_H / 2)) {
                continue;
            }
            boolean unlocked = MCH_EconomyClientData.isUnlocked(node.id);
            boolean selected = node.id.equals(this.selectedNodeId);
            boolean reqOk = hasPrerequisites(node);
            int frameU = 0;
            if (unlocked) {
                frameU = 52;
            } else if (reqOk) {
                frameU = 26;
            }
            this.drawTexturedModalRect(sx, sy, frameU, 202, NODE_W, NODE_H);
            if (selected || node.id.equals(hoverNode)) {
                int border = selected ? 0xE0F0D070 : 0x90D0D0D0;
                drawRect(sx - 1, sy - 1, sx + NODE_W + 1, sy, border);
                drawRect(sx - 1, sy + NODE_H, sx + NODE_W + 1, sy + NODE_H + 1, border);
                drawRect(sx - 1, sy, sx, sy + NODE_H, border);
                drawRect(sx + NODE_W, sy, sx + NODE_W + 1, sy + NODE_H, border);
            }
            ItemStack icon = resolveNodeIcon(node);
            if (icon != null) {
                this.itemRenderer.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), icon, sx + 5, sy + 5);
                this.itemRenderer.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), icon, sx + 5, sy + 5);
            }
            this.fontRendererObj.drawStringWithShadow(trimNodeName(node), sx + NODE_W + 4, sy + 9, unlocked ? 0xC0FFC0 : 0xE6E6E6);
        }
        RenderHelper.disableStandardItemLighting();
    }

    private void drawDetailPanel() {
        int x = this.detailLeft + 8;
        int y = this.detailTop + 24;
        this.fontRendererObj.drawStringWithShadow("货币余额", x, y, 0xF0F0F0);
        y += 14;
        drawCurrencyLine(x, y, "SL", MCH_EconomyClientData.getSL(), TEX_ICON_SL, new ItemStack(Items.gold_ingot), 0xFFE07A);
        y += 13;
        drawCurrencyLine(x, y, "GE", MCH_EconomyClientData.getGE(), TEX_ICON_GE, new ItemStack(Items.emerald), 0xFFD050);
        y += 13;
        drawCurrencyLine(x, y, "RP", MCH_EconomyClientData.getRP(), TEX_ICON_RP, new ItemStack(Items.enchanted_book), 0xA0D0FF);
        y += 18;
        MCH_EconomyTechNode node = getSelectedNode();
        if (node == null) {
            this.fontRendererObj.drawString("未选择节点", x, y, 0xD0D0D0);
            return;
        }
        this.fontRendererObj.drawStringWithShadow(trimNodeName(node), x, y, 0xFFFFFF);
        y += 12;
        this.fontRendererObj.drawString("ID: " + node.id, x, y, 0xB8B8B8);
        y += 11;
        this.fontRendererObj.drawString("类型: " + typeLabel(node.type), x, y, 0xB8B8B8);
        y += 11;
        this.fontRendererObj.drawString("花费: SL " + node.costSL + " / GE " + node.costGE + " / RP " + node.costRP, x, y, 0xE0B080);
        y += 11;
        if (node.grantSL > 0 || node.grantGE > 0 || node.grantRP > 0) {
            this.fontRendererObj.drawString("兑换: SL " + node.grantSL + " / GE " + node.grantGE + " / RP " + node.grantRP, x, y, 0x80D0A0);
            y += 11;
        }
        this.fontRendererObj.drawString("前置: " + prerequisiteText(node), x, y, hasPrerequisites(node) ? 0x90D090 : 0xD09090);
        y += 12;
        boolean unlocked = MCH_EconomyClientData.isUnlocked(node.id);
        this.fontRendererObj.drawString("状态: " + (unlocked ? "已解锁" : "未解锁"), x, y, unlocked ? 0x90E090 : 0xC8C8C8);
        y += 14;
        String msg = MCH_EconomyClientData.getLastTechMessage();
        if (msg != null && !msg.isEmpty()) {
            int color = MCH_EconomyClientData.isLastTechSuccess() ? 0x90E090 : 0xE09090;
            this.fontRendererObj.drawString(msg, x, y, color);
        }
    }

    private void drawCurrencyLine(int x, int y, String label, int value, ResourceLocation icon, ItemStack fallback, int color) {
        drawCurrencyIcon(x, y - 1, icon, fallback);
        this.fontRendererObj.drawString(label + ": " + value, x + 14, y + 1, color);
    }

    private void drawCurrencyIcon(int x, int y, ResourceLocation icon, ItemStack fallback) {
        if (bindTextureSafely(icon)) {
            GL11.glColor4f(1, 1, 1, 1);
            drawTexturedRect(x, y, 10, 10, 0, 0, 1, 1);
            return;
        }
        if (fallback != null) {
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRenderer.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), fallback, x - 3, y - 3);
            RenderHelper.disableStandardItemLighting();
        }
    }

    private void drawTexturedRect(int x, int y, int w, int h, float u0, float v0, float u1, float v1) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV((double) x, (double) (y + h), 0.0D, (double) u0, (double) v1);
        t.addVertexWithUV((double) (x + w), (double) (y + h), 0.0D, (double) u1, (double) v1);
        t.addVertexWithUV((double) (x + w), (double) y, 0.0D, (double) u1, (double) v0);
        t.addVertexWithUV((double) x, (double) y, 0.0D, (double) u0, (double) v0);
        t.draw();
    }

    private boolean bindTextureSafely(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        try {
            this.mc.getResourceManager().getResource(texture);
            this.mc.getTextureManager().bindTexture(texture);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void drawNodeTooltip(int mouseX, int mouseY) {
        String nodeId = findNodeAt(mouseX, mouseY);
        if (nodeId == null || nodeId.isEmpty()) {
            return;
        }
        MCH_EconomyTechNode node = MCH_EconomyTechRegistry.getNode(nodeId);
        if (node == null) {
            return;
        }
        List<String> lines = new ArrayList<String>();
        lines.add(trimNodeName(node));
        lines.add(typeLabel(node.type));
        lines.add("花费 SL " + node.costSL + " / GE " + node.costGE + " / RP " + node.costRP);
        if (!node.prerequisites.isEmpty()) {
            lines.add("前置: " + prerequisiteText(node));
        }
        lines.add(MCH_EconomyClientData.isUnlocked(node.id) ? "状态: 已解锁" : (hasPrerequisites(node) ? "状态: 可操作" : "状态: 前置不足"));
        int max = 0;
        for (String s : lines) {
            max = Math.max(max, this.fontRendererObj.getStringWidth(s));
        }
        int w = max + 10;
        int h = lines.size() * 10 + 6;
        int tx = mouseX + 12;
        int ty = mouseY - 10;
        if (tx + w > this.width) {
            tx = mouseX - w - 12;
        }
        if (ty + h > this.height) {
            ty = this.height - h - 6;
        }
        drawGradientRect(tx, ty, tx + w, ty + h, 0xF0202020, 0xE0101010);
        drawRect(tx, ty, tx + w, ty + 1, 0xE0B09040);
        int y = ty + 4;
        for (int i = 0; i < lines.size(); i++) {
            int c = i == 0 ? 0xFFF0E0 : 0xD8D8D8;
            this.fontRendererObj.drawStringWithShadow(lines.get(i), tx + 5, y, c);
            y += 10;
        }
    }

    private void updateButtons() {
        GuiButton action = getButton(BTN_ACTION);
        if (action == null) {
            return;
        }
        MCH_EconomyTechNode node = getSelectedNode();
        if (node == null) {
            action.enabled = false;
            action.displayString = "执行操作";
            return;
        }
        action.displayString = actionText(node);
        action.enabled = canDoAction(node);
    }

    private boolean canDoAction(MCH_EconomyTechNode node) {
        if (node == null) {
            return false;
        }
        if (!hasPrerequisites(node)) {
            return false;
        }
        if (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK && MCH_EconomyClientData.isUnlocked(node.id)) {
            return false;
        }
        if (MCH_EconomyClientData.getSL() < node.costSL) {
            return false;
        }
        if (MCH_EconomyClientData.getGE() < node.costGE) {
            return false;
        }
        if (MCH_EconomyClientData.getRP() < node.costRP) {
            return false;
        }
        return true;
    }

    private String actionText(MCH_EconomyTechNode node) {
        if (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK) {
            return MCH_EconomyClientData.isUnlocked(node.id) ? "已解锁" : "RP 解锁";
        }
        if (node.type == MCH_EconomyTechNode.NodeType.SL_PURCHASE) {
            return "SL 购买";
        }
        return "GE 兑换";
    }

    private boolean hasPrerequisites(MCH_EconomyTechNode node) {
        for (String pre : node.prerequisites) {
            if (!MCH_EconomyClientData.isUnlocked(pre)) {
                return false;
            }
        }
        return true;
    }

    private String prerequisiteText(MCH_EconomyTechNode node) {
        if (node.prerequisites == null || node.prerequisites.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (String pre : node.prerequisites) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(trimNodeName(MCH_EconomyTechRegistry.normalizeId(pre)));
        }
        return sb.toString();
    }

    private MCH_EconomyTechNode getSelectedNode() {
        if (this.selectedNodeId == null || this.selectedNodeId.isEmpty()) {
            return null;
        }
        return MCH_EconomyTechRegistry.getNode(this.selectedNodeId);
    }

    private String findNodeAt(int mouseX, int mouseY) {
        for (MCH_EconomyTechNode node : this.nodes) {
            Point p = this.nodePos.get(node.id);
            if (p == null) {
                continue;
            }
            int sx = nodeScreenX(p.x);
            int sy = nodeScreenY(p.y);
            if (mouseX >= sx && mouseX <= sx + NODE_W && mouseY >= sy && mouseY <= sy + NODE_H) {
                return node.id;
            }
        }
        return "";
    }

    private int nodeScreenX(int worldX) {
        return this.treeLeft + this.treeWidth / 2 + this.panX + worldX;
    }

    private int nodeScreenY(int worldY) {
        return this.treeTop + 26 + this.panY + worldY;
    }

    private boolean insideTree(int x, int y) {
        return x >= this.treeLeft && x <= this.treeLeft + this.treeWidth && y >= this.treeTop && y <= this.treeTop + this.treeHeight;
    }

    private GuiButton getButton(int id) {
        for (Object obj : this.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton btn = (GuiButton) obj;
                if (btn.id == id) {
                    return btn;
                }
            }
        }
        return null;
    }

    private String typeLabel(MCH_EconomyTechNode.NodeType type) {
        if (type == MCH_EconomyTechNode.NodeType.RP_UNLOCK) {
            return "RP 解锁";
        }
        if (type == MCH_EconomyTechNode.NodeType.SL_PURCHASE) {
            return "SL 购买";
        }
        return "GE 特购/兑换";
    }

    private String trimNodeName(MCH_EconomyTechNode node) {
        return trimNodeName(node == null ? "" : node.id);
    }

    private String trimNodeName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return "";
        }
        int i = nodeId.lastIndexOf('.');
        String name = i >= 0 && i < nodeId.length() - 1 ? nodeId.substring(i + 1) : nodeId;
        return name.length() > 18 ? name.substring(0, 18) : name;
    }

    private ItemStack resolveNodeIcon(MCH_EconomyTechNode node) {
        if (node == null) {
            return null;
        }
        ItemStack cached = this.nodeIconCache.get(node.id);
        if (cached != null) {
            return cached;
        }
        ItemStack stack = null;
        if (node.type == MCH_EconomyTechNode.NodeType.RP_UNLOCK) {
            stack = new ItemStack(Items.enchanted_book);
        } else if (node.type == MCH_EconomyTechNode.NodeType.GE_EXCHANGE) {
            stack = new ItemStack(Items.emerald);
        } else if (node.npcItemName != null && !node.npcItemName.isEmpty()) {
            Object obj = Item.itemRegistry.getObject(node.npcItemName);
            if (!(obj instanceof Item)) {
                obj = Item.itemRegistry.getObject("mcheli:" + node.npcItemName);
            }
            if (obj instanceof Item) {
                stack = new ItemStack((Item) obj);
            }
        }
        if (stack == null) {
            stack = new ItemStack(Items.name_tag);
        }
        this.nodeIconCache.put(node.id, stack);
        return stack;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
