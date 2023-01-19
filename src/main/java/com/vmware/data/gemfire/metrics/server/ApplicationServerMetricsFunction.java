package com.vmware.data.gemfire.metrics.server;

import lombok.extern.slf4j.Slf4j;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.execute.DefaultResultCollector;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ApplicationServerMetricsFunction implements Function {
    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public void execute(FunctionContext functionContext) {
        String[] params = (String[]) functionContext.getArguments();
        InternalCache cache = (InternalCache) CacheFactory.getAnyInstance();
        Map<InternalDistributedMember, Collection<String>> locators = cache.getInternalDistributedSystem().getDistributionManager().getAllHostedLocators();
        Set<DistributedMember> members = CacheFactory.getAnyInstance().getDistributedSystem().getAllOtherMembers();
        members.add(CacheFactory.getAnyInstance().getDistributedSystem().getDistributedMember());
        locators.forEach((a, b) -> {
            if (members.contains(a)) {
                members.remove(a);
            }
        });
        ResultCollector jmxResults = FunctionService.onMembers(members).withCollector(new DefaultResultCollector())
                .setArguments(params).execute("ApplicationServerMetricsJMXFunction");
        functionContext.getResultSender().lastResult(jmxResults.getResult());
        log.info("ApplicationServerMetricsFunction completed {}", jmxResults);
    }

    @Override
    public String getId() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean optimizeForWrite() {
        return false;
    }

    @Override
    public boolean isHA() {
        return false;
    }
}
