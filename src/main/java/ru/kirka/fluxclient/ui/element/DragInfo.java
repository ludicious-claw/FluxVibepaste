package ru.kirka.fluxclient.ui.element;

import ru.kirka.fluxclient.FluxClient;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.ui.widget.Widget;
import ru.kirka.fluxclient.util.MathUtil;

public class DragInfo implements Interface {
   private final String name;
   private Widget widget;
   private float x;
   private float y;
   private float width;
   private float height;
   private double offsetX = 0.0;
   private double offsetY = 0.0;
   private int dragStatus = 0;

   public DragInfo(String name, float x, float y, float width, float height) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      if (FluxClient.getInstance() != null && FluxClient.getInstance().getDragProcessor() != null) {
         FluxClient.getInstance().getDragProcessor().getElements().add(this);
      }
   }

   public Widget getWidget() {
      return this.widget;
   }

   public void setWidget(Widget widget) {
      this.widget = widget;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public double getOffsetX() {
      return this.offsetX;
   }

   public void setOffsetX(double offsetX) {
      this.offsetX = offsetX;
   }

   public double getOffsetY() {
      return this.offsetY;
   }

   public void setOffsetY(double offsetY) {
      this.offsetY = offsetY;
   }

   public String getName() {
      return this.name;
   }

   public int getDragStatus() {
      return this.dragStatus;
   }

   public void setDragStatus(int status) {
      this.dragStatus = status;
   }

   public float getClampedX() {
      if (mc != null && mc.getWindow() != null) {
         float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
         return MathUtil.b(this.x, 0.0F, Math.max(0.0F, screenW - this.width));
      } else {
         return this.x;
      }
   }

   public float getClampedY() {
      if (mc != null && mc.getWindow() != null) {
         float screenH = (float)mc.getWindow().getFramebufferHeight() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
         return MathUtil.b(this.y, 0.0F, Math.max(0.0F, screenH - this.height));
      } else {
         return this.y;
      }
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }
}
