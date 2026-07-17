package mcheli.aircraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mcheli.wrapper.modelloader.W_MetasequoiaObject;
import mcheli.wrapper.modelloader.W_WavefrontObject;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
final class MCH_ModelDisplayListCache {

    private static final Map<IModelCustom, ModelLists> MODEL_LISTS = new IdentityHashMap<IModelCustom, ModelLists>();

    private MCH_ModelDisplayListCache() {
    }

    static synchronized boolean renderAll(IModelCustom model) {
        if (!isSupported(model)) {
            return false;
        }

        ModelLists lists = getLists(model);
        if (lists.renderAll == 0) {
            lists.renderAll = compileAll(model);
            if (lists.renderAll == 0) {
                return false;
            }
        }

        GL11.glCallList(lists.renderAll);
        return true;
    }

    static synchronized boolean renderPart(IModelCustom model, String partName) {
        if (!isSupported(model) || partName == null) {
            return false;
        }

        ModelLists lists = getLists(model);
        Integer cachedList = lists.parts.get(partName);
        int list = cachedList != null ? cachedList.intValue() : 0;
        if (list == 0) {
            list = compilePart(model, partName);
            if (list == 0) {
                return false;
            }
            lists.parts.put(partName, Integer.valueOf(list));
        }

        GL11.glCallList(list);
        return true;
    }

    static synchronized void clear() {
        for (ModelLists lists : MODEL_LISTS.values()) {
            if (lists.renderAll != 0) {
                GL11.glDeleteLists(lists.renderAll, 1);
            }
            for (Integer list : lists.parts.values()) {
                if (list != null && list.intValue() != 0) {
                    GL11.glDeleteLists(list.intValue(), 1);
                }
            }
        }
        MODEL_LISTS.clear();
    }

    private static boolean isSupported(IModelCustom model) {
        return model instanceof W_WavefrontObject || model instanceof W_MetasequoiaObject;
    }

    private static ModelLists getLists(IModelCustom model) {
        ModelLists lists = MODEL_LISTS.get(model);
        if (lists == null) {
            lists = new ModelLists();
            MODEL_LISTS.put(model, lists);
        }
        return lists;
    }

    private static int compileAll(IModelCustom model) {
        int list = GL11.glGenLists(1);
        if (list == 0) {
            return 0;
        }

        boolean listOpen = false;
        boolean compiled = false;
        try {
            GL11.glNewList(list, GL11.GL_COMPILE);
            listOpen = true;
            model.renderAll();
            listOpen = false;
            GL11.glEndList();
            compiled = true;
            return list;
        } finally {
            try {
                if (listOpen) {
                    GL11.glEndList();
                }
            } finally {
                if (!compiled) {
                    GL11.glDeleteLists(list, 1);
                }
            }
        }
    }

    private static int compilePart(IModelCustom model, String partName) {
        int list = GL11.glGenLists(1);
        if (list == 0) {
            return 0;
        }

        boolean listOpen = false;
        boolean compiled = false;
        try {
            GL11.glNewList(list, GL11.GL_COMPILE);
            listOpen = true;
            model.renderPart(partName);
            listOpen = false;
            GL11.glEndList();
            compiled = true;
            return list;
        } finally {
            try {
                if (listOpen) {
                    GL11.glEndList();
                }
            } finally {
                if (!compiled) {
                    GL11.glDeleteLists(list, 1);
                }
            }
        }
    }

    private static final class ModelLists {
        private int renderAll;
        private final Map<String, Integer> parts = new HashMap<String, Integer>();
    }
}
