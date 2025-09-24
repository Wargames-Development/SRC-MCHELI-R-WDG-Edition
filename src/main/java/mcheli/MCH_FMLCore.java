package mcheli;

import com.falsepattern.gasstation.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MCH_FMLCore implements IEarlyMixinLoader, IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public @Nullable String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return "";
    }

    @Override
    public List<String> getMixinConfigs() {
        String[] configs = {"mixins.mcheli.json"};
        return Arrays.asList(configs);
    }
}
