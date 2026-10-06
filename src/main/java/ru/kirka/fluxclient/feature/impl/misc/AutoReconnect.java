package ru.kirka.fluxclient.feature.impl.misc;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import ru.kirka.fluxclient.config.impl.NumberSetting;
import ru.kirka.fluxclient.feature.Category;
import ru.kirka.fluxclient.feature.Module;

public class AutoReconnect extends Module {
   public final NumberSetting delay = new NumberSetting("Задержка", "Секунд до переподключения", 5.0F, 1.0F, 30.0F, 1.0F);
   private long disconnectTime = 0L;

   public AutoReconnect() {
      super("AutoReconnect", "Сам заходит на сервер после дисконнекта", Category.MISC, 0);
      this.registerSetting(this.delay);
   }

   @Override
   public void onTick() {
      if (mc.world != null) {
         this.disconnectTime = 0L;
      } else {
         if (mc.currentScreen instanceof DisconnectedScreen && mc.getCurrentServerEntry() != null) {
            if (this.disconnectTime == 0L) {
               this.disconnectTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - this.disconnectTime >= (long)(this.delay.get() * 1000.0F)) {
               this.disconnectTime = 0L;
               ServerInfo info = mc.getCurrentServerEntry();
               ServerAddress addr = ServerAddress.parse(info.address);
               ConnectScreen.connect(new TitleScreen(), mc, addr, info, false, null);
            }
         } else {
            this.disconnectTime = 0L;
         }
      }
   }
}
