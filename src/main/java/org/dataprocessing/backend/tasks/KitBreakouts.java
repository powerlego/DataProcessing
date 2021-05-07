package org.dataprocessing.backend.tasks;

import javafx.concurrent.Task;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dataprocessing.backend.objects.Subassembly;
import org.dataprocessing.backend.objects.Subassembly.AssemblyItem;
import org.dataprocessing.utils.CustomExecutors;
import org.dataprocessing.utils.SynchronizedMultiValuedMap;
import org.dataprocessing.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Nicholas Curl
 */
public class KitBreakouts extends Task<MultiValuedMap<String, List<?>>> {

    /**
     * The instance of the logger
     */
    private static final Logger                   logger = LogManager.getLogger(KitBreakouts.class);
    private static final Utils                    utils  = Utils.getInstance();
    private              Map<String, Subassembly> subassemblies;

    private void breakoutRecurse(String parentKey,
                                 Subassembly subassembly,
                                 Map<String, Subassembly> subassemblies,
                                 MultiValuedMap<String, List<?>> breakoutMap,
                                 double qty
    ) {
        ExecutorService executorService = CustomExecutors.newFixedThreadPool(5);
        if (subassembly != null) {
            for (AssemblyItem assemblyItem : subassembly.getAssemblyItems()) {
                executorService.submit(() -> {
                    List<Object> row = new ArrayList<>();
                    row.add(parentKey);
                    row.add(subassembly.getAssemblyKey());
                    row.add(assemblyItem.getItemKey());
                    row.add(assemblyItem.getName());
                    row.add(assemblyItem.getQty());
                    row.add(assemblyItem.getQty() * qty);
                    breakoutMap.put(parentKey, row);
                    if (assemblyItem.isSubassembly()) {
                        Subassembly childAssembly = subassemblies.get(assemblyItem.getItemKey());
                        breakoutRecurse(parentKey,
                                        childAssembly,
                                        subassemblies,
                                        breakoutMap,
                                        qty * assemblyItem.getQty()
                        );
                    }
                    return null;
                });
            }
        }
        utils.shutdownExecutor(executorService, logger);
    }

    /**
     * Invoked when the Task is executed, the call method must be overridden and
     * implemented by subclasses. The call method actually performs the
     * background thread logic. Only the updateProgress, updateMessage, updateValue and
     * updateTitle methods of Task may be called from code within this method.
     * Any other interaction with the Task from the background thread will result
     * in runtime exceptions.
     *
     * @return The result of the background work, if any.
     *
     * @throws Exception an unhandled exception which occurred during the
     *                   background operation
     */
    @Override
    protected MultiValuedMap<String, List<?>> call() throws Exception {
        MultiValuedMap<String, List<?>> breakout = new SynchronizedMultiValuedMap<>(new ArrayListValuedHashMap<>());
        AtomicReference<Double> progress = new AtomicReference<>(0.0);
        updateProgress(progress.get(), 1.0);
        double progressUpdate = 1.0 / subassemblies.size();
        ExecutorService executorService = CustomExecutors.newFixedThreadPool(20);
        for (Subassembly subassembly : subassemblies.values()) {
            executorService.submit(() -> {
                List<Object> row = new ArrayList<>();
                row.add(subassembly.getAssemblyKey());
                row.add("");
                row.add(subassembly.getAssemblyKey());
                row.add(subassembly.getName());
                row.add(1.0);
                row.add(1.0);
                breakout.put(subassembly.getAssemblyKey(), row);
                breakoutRecurse(subassembly.getAssemblyKey(), subassembly, subassemblies, breakout, 1.0);
                progress.updateAndGet(v -> v + progressUpdate);
                updateProgress(progress.get(), 1.0);
                return null;
            });
        }
        utils.shutdownExecutor(executorService, logger);
        return breakout;
    }

    public void setSubassemblies(Map<String, Subassembly> subassemblies) {
        this.subassemblies = new ConcurrentHashMap<>(subassemblies);
    }
}
