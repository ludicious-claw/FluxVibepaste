package ru.kirka.fluxclient.event;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.render.Draw2DProcessor;
import ru.kirka.fluxclient.render.Draw3DProcessor;

public class DrawEvent extends ru.kirka.fluxclient.core.Event implements Interface {
   private final DrawEvent.Type type;
   private final float tickDelta;
   private final MatrixStack matrixStack;
   private DrawContext drawContext;

   public DrawEvent(MatrixStack stack, float tickDelta, DrawEvent.Type type) {
      this.matrixStack = stack;
      this.tickDelta = tickDelta;
      this.type = type;
   }

   public DrawEvent(DrawContext context, float tickDelta, DrawEvent.Type type) {
      this.drawContext = context;
      this.matrixStack = context.getMatrices();
      this.tickDelta = tickDelta;
      this.type = type;
   }

   public Draw2DProcessor getDraw2DProcessor() {
      return FluxClient.getInstance().getDraw2DProcessor();
   }

   public Draw3DProcessor getDraw3DProcessor() {
      return FluxClient.getInstance().getDraw3DProcessor();
   }

   public DrawEvent.Type getType() {
      return this.type;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public float g() {
      return this.tickDelta;
   }

   public MatrixStack getMatrixStack() {
      return this.matrixStack;
   }

   public MatrixStack h() {
      return this.matrixStack;
   }

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public DrawContext i() {
      return this.drawContext;
   }

   public boolean is2D() {
      return this.type == DrawEvent.Type.D2D;
   }

   public boolean b() {
      return this.type == DrawEvent.Type.D2D;
   }

   public boolean is3D() {
      return this.type == DrawEvent.Type.D3D;
   }

   public boolean c() {
      return this.type == DrawEvent.Type.D3D;
   }

   public static enum Type {
      D2D,
      D3D;
   }
}
