package ru.kirka.fluxclient.ui.element;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.kirka.fluxclient.common.Interface;
import ru.kirka.fluxclient.core.EventTarget;
import ru.kirka.fluxclient.event.ClickEvent;
import ru.kirka.fluxclient.event.DrawEvent;
import ru.kirka.fluxclient.render.AnimationUtil;
import ru.kirka.fluxclient.render.ColorUtil;
import ru.kirka.fluxclient.render.EasingList;
import ru.kirka.fluxclient.util.CursorUtil;
import ru.kirka.fluxclient.util.MathUtil;

public class DragProcessor implements Interface {
   private final List<DragInfo> elements = new ArrayList<>();
   private final DragProcessor.SnapGuide snapX = new DragProcessor.SnapGuide();
   private final DragProcessor.SnapGuide snapY = new DragProcessor.SnapGuide();
   private DragInfo activeDragInfo = null;

   public List<DragInfo> getElements() {
      return this.elements;
   }

   public DragProcessor.SnapGuide getSnapGuideX() {
      return this.snapX;
   }

   public DragProcessor.SnapGuide getSnapGuideY() {
      return this.snapY;
   }

   public DragInfo getActiveDragInfo() {
      return this.activeDragInfo;
   }

   @EventTarget
   public void onClick(ClickEvent event) {
      if (mc.currentScreen instanceof ChatScreen) {
         if (event.isPress() && event.getButton() == 0) {
            for (DragInfo dragInfo : this.elements) {
               if (dragInfo.getDragStatus() != 2
                  && MathUtil.isHovered(
                     event.getMouseX(), event.getMouseY(), dragInfo.getClampedX(), dragInfo.getClampedY(), dragInfo.getWidth(), dragInfo.getHeight()
                  )) {
                  CursorUtil.setCursor(CursorUtil.CursorType.HAND);
                  this.activeDragInfo = dragInfo;
                  this.activeDragInfo.setOffsetX(event.getMouseX() - dragInfo.getClampedX());
                  this.activeDragInfo.setOffsetY(event.getMouseY() - dragInfo.getClampedY());
                  break;
               }
            }
         } else if (event.isRelease() && event.getButton() == 0) {
            this.resetDrag();
         } else if (event.isDrag() && this.activeDragInfo != null && event.getButton() == 0) {
            this.updateDragPosition(
               (float)(event.getMouseX() - this.activeDragInfo.getOffsetX()),
               (float)(event.getMouseY() - this.activeDragInfo.getOffsetY()),
               this.activeDragInfo
            );
         }

         for (DragInfo dragInfo2 : this.elements) {
            if (dragInfo2.getWidget() != null) {
               if (event.getButton() == 1 && event.isPress()) {
                  if (MathUtil.isHovered(
                     event.getMouseX(), event.getMouseY(), dragInfo2.getClampedX(), dragInfo2.getClampedY(), dragInfo2.getWidth(), dragInfo2.getHeight()
                  )) {
                     dragInfo2.getWidget().a(!dragInfo2.getWidget().g());
                  }
               } else if (event.getButton() == 0 && event.isRelease() && dragInfo2.getWidget().g()) {
                  for (Element<?> element : dragInfo2.getWidget().c()) {
                     element.onMouseClick(event.getMouseX(), event.getMouseY(), event.getButton());
                  }
               }
            }
         }
      }
   }

   @EventTarget(a = 4)
   public void onDraw(DrawEvent event) {
      if (event.is2D()) {
         if (mc.currentScreen instanceof ChatScreen) {
            if (this.activeDragInfo == null && !this.snapX.isActive() && !this.snapY.isActive()) {
               this.resetDrag();
            }

            this.snapX.update(this.activeDragInfo != null, event.getTickDelta());
            this.snapY.update(this.activeDragInfo != null, event.getTickDelta());
            if (this.snapX.isActive() || this.snapY.isActive()) {
               this.drawSnapGuides(event);
            }

            return;
         }

         for (DragInfo dragInfo : this.elements) {
            if (dragInfo.getWidget() != null) {
               dragInfo.getWidget().a(false);
            }
         }

         if (this.activeDragInfo != null) {
            this.resetDrag();
         }
      }
   }

   private void updateDragPosition(float x, float y, DragInfo dragInfo) {
      int status = dragInfo.getDragStatus();
      if (status == 2) {
         this.snapX.setTarget(null);
         this.snapY.setTarget(null);
      } else {
         boolean onlyY = status == 1;
         if (onlyY) {
            x = dragInfo.getClampedX();
         }

         float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
         float screenH = (float)mc.getWindow().getFramebufferHeight() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
         float x2 = MathUtil.b(x, 0.0F, Math.max(0.0F, screenW - dragInfo.getWidth()));
         float y2 = MathUtil.b(y, 0.0F, Math.max(0.0F, screenH - dragInfo.getHeight()));
         if (!onlyY) {
            x2 = this.snapToGuide(DragProcessor.Axis.X, x2, dragInfo);
         } else {
            this.snapX.setTarget(null);
         }

         float y3 = this.snapToGuide(DragProcessor.Axis.Y, y2, dragInfo);
         dragInfo.setX(MathUtil.b(x2, 0.0F, Math.max(0.0F, screenW - dragInfo.getWidth())));
         dragInfo.setY(MathUtil.b(y3, 0.0F, Math.max(0.0F, screenH - dragInfo.getHeight())));
      }
   }

   private float snapToGuide(DragProcessor.Axis axis, float pos, DragInfo dragInfo) {
      float size = axis.getSize(dragInfo);
      float[] points = new float[]{pos, pos + size / 2.0F, pos + size};
      DragProcessor.SnapGuide guide = axis == DragProcessor.Axis.X ? this.snapX : this.snapY;
      float bestDistance = 25.0F;
      Float bestGuide = null;
      float snappedPos = pos;

      for (float guidePos : this.getGuidePoints(axis, dragInfo)) {
         for (int i = 0; i < points.length; i++) {
            float distance = Math.abs(points[i] - guidePos);
            if (distance < bestDistance) {
               bestDistance = distance;
               bestGuide = guidePos;
               float offset = i == 0 ? 0.0F : (i == 1 ? size / 2.0F : size);
               snappedPos = guidePos - offset;
            }
         }
      }

      if (bestGuide != null && bestDistance < 5.0F) {
         pos = snappedPos;
         guide.setTarget(bestGuide);
      } else {
         guide.setTarget(null);
      }

      return pos;
   }

   private List<Float> getGuidePoints(DragProcessor.Axis axis, DragInfo currentElement) {
      List<Float> guides = new ArrayList<>();
      guides.add(0.0F);
      guides.add(axis.screenSize() / 2.0F);
      guides.add(axis.screenSize());

      for (DragInfo other : this.elements) {
         if (other != currentElement && (other.getWidth() != 0.0F || other.getHeight() != 0.0F)) {
            float pos = axis.getPosition(other);
            float size = axis.getSize(other);
            guides.add(pos);
            guides.add(pos + size / 2.0F);
            guides.add(pos + size);
         }
      }

      return guides;
   }

   private void drawSnapGuides(DrawEvent event) {
      float screenW = (float)mc.getWindow().getFramebufferWidth() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
      float screenH = (float)mc.getWindow().getFramebufferHeight() / mc.getWindow().calculateScaleFactor(2, mc.forcesUnicodeFont());
      if (this.snapX.isActive()) {
         event.getDraw2DProcessor()
            .a(
               event.getDrawContext(),
               this.snapX.getPosition() - 0.5F,
               0.0F,
               0.5F,
               screenH,
               ColorUtil.convertToARGB(255, 255, 255, (int)(this.snapX.getAnimation().c() * 200.0F))
            );
      }

      if (this.snapY.isActive()) {
         event.getDraw2DProcessor()
            .a(
               event.getDrawContext(),
               0.0F,
               this.snapY.getPosition() - 0.5F,
               screenW,
               0.5F,
               ColorUtil.convertToARGB(255, 255, 255, (int)(this.snapY.getAnimation().c() * 200.0F))
            );
      }
   }

   private void resetDrag() {
      CursorUtil.setCursor(CursorUtil.CursorType.DEFAULT);
      this.activeDragInfo = null;
      this.snapY.setTarget(null);
      this.snapX.setTarget(null);
   }

   static enum Axis {
      X,
      Y;

      float screenSize() {
         if (Interface.mc != null && Interface.mc.getWindow() != null) {
            return this == X
               ? (float)Interface.mc.getWindow().getFramebufferWidth() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont())
               : (float)Interface.mc.getWindow().getFramebufferHeight() / Interface.mc.getWindow().calculateScaleFactor(2, Interface.mc.forcesUnicodeFont());
         } else {
            return 0.0F;
         }
      }

      float getPosition(DragInfo info) {
         return this == X ? info.getClampedX() : info.getClampedY();
      }

      float getSize(DragInfo info) {
         return this == X ? info.getWidth() : info.getHeight();
      }
   }

   public static class SnapGuide {
      private final AnimationUtil a = new AnimationUtil();
      private Float targetPos = null;
      private boolean active = false;

      public AnimationUtil getAnimation() {
         return this.a;
      }

      public Float getPosition() {
         return this.targetPos;
      }

      public void setTarget(Float newPosition) {
         if (newPosition == null) {
            this.active = false;
         } else if (!newPosition.equals(this.targetPos)) {
            this.targetPos = newPosition;
            this.active = true;
         }
      }

      public void update(boolean dragging, float tickDelta) {
         if (this.targetPos != null) {
            boolean should = dragging && this.active;
            this.a.a(should);
            this.a.a(0.0F, 1.0F, 0.3F, EasingList.i, tickDelta);
            if (!should && this.a.c() <= 0.0F) {
               this.targetPos = null;
            }
         }
      }

      public boolean isActive() {
         return this.targetPos != null && this.a.c() > 0.0F;
      }
   }
}
