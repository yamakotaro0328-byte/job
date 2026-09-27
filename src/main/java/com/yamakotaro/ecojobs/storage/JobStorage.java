package com.yamakotaro.ecojobs.storage;

import com.yamakotaro.ecojobs.PlayerJobData;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface JobStorage {
   Map<UUID, PlayerJobData> loadAll();

   void saveAll(Map<UUID, PlayerJobData> var1, Set<UUID> var2);

   void close();
}
