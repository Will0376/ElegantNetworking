package z.hohserg.elegant.networking.impl;

import com.google.common.collect.SetMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.gui.ChatFormatting;
import z.hohserg.elegant.networking.api.ClientToServerPacket;
import z.hohserg.elegant.networking.api.ServerToClientPacket;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.*;

@Mod(modid = "elegant_networking", name = "ElegantNetworking", version = "1.0")
public class Main {

    private static Set<String> channelsToRegister = new HashSet<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws ClassNotFoundException {
        for (ModContainer modContainer : Loader.instance().getActiveModList()) {
            SetMultimap<String, ASMDataTable.ASMData> annotationsFor = event.getAsmData().getAnnotationsFor(modContainer);
            if (annotationsFor != null) {
                Set<ASMDataTable.ASMData> asmData = annotationsFor.get("z.hohserg.elegant.networking.api.ElegantPacket");
                Comparator<ASMDataTable.ASMData> comparing = getAsmDataComparator(modContainer.getSource());

                List<ASMDataTable.ASMData> rawPackets =
                        asmData.stream()
                                .filter(a -> {
                                    try {
                                        return Arrays.stream(Class.forName(a.getClassName()).getInterfaces()).anyMatch(i -> i == ClientToServerPacket.class || i == ServerToClientPacket.class);
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                        return false;
                                    }
                                })
                                .sorted(comparing)
                                .collect(toList());

                if (rawPackets.size() > 0) {

                    List<ElegantNetworking.PacketInfo> packets =
                            rawPackets.stream()
                                    .map(a -> new ElegantNetworking.PacketInfo((String) a.getAnnotationInfo().getOrDefault("channel", modContainer.getModId()), a.getClassName()))
                                    .collect(toList());

                    packets.stream().map(p -> p.channel).forEach(channelsToRegister::add);

                    for (int i = 0; i < packets.size(); i++) {
                        ElegantNetworking.PacketInfo p = packets.get(i);
                        int id = i + 1;
                        System.out.println("Register packet " + ChatFormatting.AQUA + Class.forName(p.className).getSimpleName() + ChatFormatting.RESET + " for channel " + ChatFormatting.AQUA + p.channel + ChatFormatting.RESET + " with id " + id);
                        ElegantNetworking.register(p, id);
                    }
                }
            }
        }
    }


    private Comparator<ASMDataTable.ASMData> getAsmDataComparator(File source) {
        if (source.isFile()) {
            try {
                ZipFile zipFile = new ZipFile(source);
                ZipEntry entry = zipFile.getEntry("META-INF/fml_cache_annotation.json");


                if (entry != null) {
                    String jsonContent = IOUtils.toString(zipFile.getInputStream(entry), StandardCharsets.UTF_8);

                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(jsonContent).getAsJsonObject();
                    Set<String> classNames = obj.entrySet().stream().map(Map.Entry::getKey).collect(toSet());

                    List<String> classOrder =
                            Arrays.stream(jsonContent.split("\\r?\\n"))
                                    .filter(l -> l.matches("  \\\".+\\\": \\{"))
                                    .filter(classNames::contains)
                                    .collect(toList());
                    Map<String, Integer> indexByClass = IntStream.range(0, classOrder.size())
                            .boxed()
                            .collect(toMap(classOrder::get, Function.identity()));
                    System.out.println(classOrder);
                    return Comparator.comparing(o -> indexByClass.get(o.getClassName()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Comparator.comparing(ASMDataTable.ASMData::getClassName);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        channelsToRegister.forEach(ElegantNetworking.getNetwork()::registerChannel);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
