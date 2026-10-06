package ru.kirka.fluxclient.staff;

import java.util.ArrayList;
import java.util.List;

public class StaffProcessor {
   private final List<StaffConstructor> staffList = new ArrayList<>();

   public List<StaffConstructor> getStaffList() {
      return this.staffList;
   }

   public List<StaffConstructor> a() {
      return this.staffList;
   }

   public void addStaff(String name) {
      if (this.staffList.stream().noneMatch(s -> s.getName().equalsIgnoreCase(name))) {
         this.staffList.add(new StaffConstructor(name));
      }
   }

   public void removeStaff(String name) {
      this.staffList.removeIf(s -> s.getName().equalsIgnoreCase(name));
   }
}
