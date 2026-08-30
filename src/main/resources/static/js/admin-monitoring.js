function monitoringHeaders() {
    return {'Authorization': 'Bearer ' + (localStorage.getItem('token') || '')};
}

function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>'"]/g, character => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[character]));
}

function money(value) {
    const number = Number(value || 0);
    return (number < 0 ? '-¥' : '¥') + Math.abs(number).toLocaleString('zh-CN', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

function percent(value) {
    return Number(value || 0).toFixed(1) + '%';
}

function checkAdminLogin() {
    const admin = JSON.parse(localStorage.getItem('admin') || 'null');
    if (!admin || admin.role !== 1) {
        alert('请先登录管理员账号');
        location.href = '/pages/admin/admin-login.html';
        return false;
    }
    document.getElementById('adminName').textContent = admin.nickname || admin.username;
    return true;
}

async function loadDecisionDashboard() {
    const button = document.getElementById('refreshBtn');
    button.disabled = true;
    try {
        const response = await fetch('/api/dashboard/decision', {headers: monitoringHeaders()});
        const result = await response.json();
        if (result.code !== 200) throw new Error(result.message || '经营数据加载失败');
        renderDashboard(result.data);
    } catch (error) {
        document.getElementById('decisionList').innerHTML =
            '<div class="decision-item danger"><i class="decision-dot"></i><div class="decision-copy"><strong>大屏加载失败</strong><span>'
            + escapeHtml(error.message) + '</span></div></div>';
    } finally {
        button.disabled = false;
    }
}

function renderDashboard(data) {
    window.dashboardOperations = data.operations || {};
    document.getElementById('generatedAt').textContent =
        '更新于 ' + String(data.generatedAt || '').replace('T', ' ').slice(0, 19);
    renderDecisions(data.decisions || []);
    renderKpis(data);
    renderOccupancy(data.occupancy || {});
    renderPricing(data.pricing || {});
    renderProtection(data.protection || {});
    renderApi(data.api || {});
}

function renderDecisions(decisions) {
    document.getElementById('decisionList').innerHTML = decisions.map(item =>
        '<article class="decision-item ' + escapeHtml(item.level) + '"><i class="decision-dot"></i>'
        + '<div class="decision-copy"><strong>' + escapeHtml(item.title) + '</strong><span>'
        + escapeHtml(item.message) + '</span></div><a class="decision-action" href="'
        + escapeHtml(item.actionUrl) + '">' + escapeHtml(item.actionLabel) + ' →</a></article>'
    ).join('');
}

function renderKpis(data) {
    const occupancy = data.occupancy || {};
    const pricing = data.pricing || {};
    const protection = data.protection || {};
    const api = data.api || {};
    document.getElementById('todayOccupancy').textContent = percent(occupancy.todayRate);
    document.getElementById('todayOccupancyHint').textContent =
        (occupancy.readyRooms || 0) + ' 间当前可售，' + (occupancy.cleaningRooms || 0) + ' 间待清洁';
    document.getElementById('pricingImpact').textContent = money(pricing.netImpact);
    document.getElementById('pricingImpactHint').textContent =
        (pricing.smartBookings || 0) + ' 笔智能定价订单，影响率 ' + percent(pricing.impactRate);
    document.getElementById('protectedRequests').textContent = protection.protectedRequests7d || 0;
    document.getElementById('protectedRequestsHint').textContent =
        (protection.inventoryConflicts7d || 0) + ' 次库存冲突，'
        + (protection.idempotentReplays7d || 0) + ' 次安全重放';
    document.getElementById('apiP95').textContent = (api.sampleCount || 0) ? api.p95Ms + ' ms' : '待采样';
    document.getElementById('apiP95Hint').textContent =
        (api.sampleCount || 0) + ' 个请求样本，错误率 ' + percent((api.errorRate || 0) * 100);
}

function renderOccupancy(occupancy) {
    document.getElementById('averageOccupancy').textContent = percent(occupancy.average14DayRate);
    const days = occupancy.days || [];
    document.getElementById('occupancyChart').innerHTML = days.map(day => {
        const rate = Number(day.occupancyRate || 0);
        const level = rate >= 80 ? 'high' : rate <= 30 ? 'low' : 'normal';
        const title = day.date + '：已订 ' + day.occupiedRooms + ' 间，可用库存 '
            + day.availableInventory + ' 间，入住率 ' + percent(rate);
        return '<div class="chart-column" title="' + escapeHtml(title) + '"><span class="chart-value">'
            + Math.round(rate) + '%</span><div class="chart-bar-slot"><i class="chart-bar ' + level
            + '" style="height:' + Math.max(2, rate) + '%"></i></div><span class="chart-label"><strong>'
            + escapeHtml(day.label) + '</strong>' + escapeHtml(day.weekday) + '</span></div>';
    }).join('');

    const capacity = Number(occupancy.capacityRooms || 0);
    const ready = Number(occupancy.readyRooms || 0);
    const angle = capacity ? ready / capacity * 360 : 0;
    const ring = document.getElementById('inventoryRing');
    ring.style.setProperty('--ready-angle', angle + 'deg');
    ring.innerHTML = '<strong>' + ready + ' / ' + capacity + '</strong><span>可售 / 容量</span>';
    document.getElementById('cleaningRooms').textContent = occupancy.cleaningRooms || 0;
    document.getElementById('overdueTasks').textContent =
        window.dashboardOperations ? window.dashboardOperations.overdueTasks : '--';
    document.getElementById('peakOccupancy').textContent =
        (occupancy.peakDate || '--') + ' · ' + percent(occupancy.peakRate);
}

function renderPricing(pricing) {
    document.getElementById('pricingCoverage').textContent = percent(pricing.coverageRate);
    document.getElementById('baselineAmount').textContent = money(pricing.baselineAmount);
    document.getElementById('bookedAmount').textContent = money(pricing.bookedAmount);
    const baseline = Math.abs(Number(pricing.baselineAmount || 0));
    const booked = Math.abs(Number(pricing.bookedAmount || 0));
    const maximum = Math.max(baseline, booked, 1);
    document.getElementById('baselineBar').style.width = (baseline / maximum * 100) + '%';
    document.getElementById('bookedBar').style.width = (booked / maximum * 100) + '%';
    document.getElementById('increasedBookings').textContent = pricing.increasedBookings || 0;
    document.getElementById('discountedBookings').textContent = pricing.discountedBookings || 0;
    document.getElementById('unchangedBookings').textContent = pricing.unchangedBookings || 0;
}

function renderProtection(protection) {
    document.getElementById('inventoryConflicts').textContent = protection.inventoryConflicts7d || 0;
    const trend = protection.trend || [];
    const maximum = Math.max(1, ...trend.flatMap(point => [Number(point.conflicts || 0), Number(point.replays || 0)]));
    document.getElementById('protectionChart').innerHTML = trend.map(point => {
        const conflicts = Number(point.conflicts || 0);
        const replays = Number(point.replays || 0);
        const title = point.date + '：冲突 ' + conflicts + '，幂等重放 ' + replays;
        return '<div class="chart-column" title="' + escapeHtml(title) + '"><span class="chart-value">'
            + (conflicts + replays) + '</span><div class="paired-bars"><i class="conflict" style="height:'
            + Math.max(conflicts ? 8 : 2, conflicts / maximum * 100) + '%"></i><i class="replay" style="height:'
            + Math.max(replays ? 8 : 2, replays / maximum * 100) + '%"></i></div><span class="chart-label"><strong>'
            + escapeHtml(point.label) + '</strong></span></div>';
    }).join('');
}

function renderApi(api) {
    window.dashboardOperations = window.dashboardOperations || {};
    const health = document.getElementById('apiHealth');
    const healthMap = {HEALTHY: ['healthy', '健康'], WARNING: ['warning', '需关注'], NO_DATA: ['no-data', '待采样']};
    const state = healthMap[api.health] || healthMap.NO_DATA;
    health.className = 'health-badge ' + state[0];
    health.textContent = state[1];
    document.getElementById('apiWindowDescription').textContent =
        '最近 ' + (api.windowMinutes || 15) + ' 分钟、最多 5000 个请求的进程内窗口，应用重启后重新采样。';
    document.getElementById('apiThresholdLegend').textContent = '告警阈值 ' + (api.slowThresholdMs || 0) + ' ms';
    document.getElementById('apiSamples').textContent = api.sampleCount || 0;
    document.getElementById('apiP50').textContent = (api.sampleCount || 0) ? api.p50Ms + ' ms' : '--';
    document.getElementById('apiP99').textContent = (api.sampleCount || 0) ? api.p99Ms + ' ms' : '--';
    document.getElementById('apiErrorRate').textContent = percent((api.errorRate || 0) * 100);

    const trend = api.trend || [];
    const threshold = Number(api.slowThresholdMs || 500);
    const maximum = Math.max(threshold, ...trend.map(point => Number(point.p95Ms || 0)), 1);
    document.getElementById('apiTrend').innerHTML = trend.map(point => {
        const latency = Number(point.p95Ms || 0);
        const warning = latency > threshold ? ' warning' : '';
        return '<div class="chart-column" title="' + escapeHtml(point.label + '：P95 ' + latency
            + ' ms，样本 ' + point.count) + '"><span class="chart-value">' + latency
            + '</span><div class="chart-bar-slot"><i class="chart-bar api-bar' + warning + '" style="height:'
            + Math.max(latency ? 5 : 2, latency / maximum * 100) + '%"></i></div><span class="chart-label"><strong>'
            + escapeHtml(point.label) + '</strong>' + point.count + '次</span></div>';
    }).join('');

    const endpoints = api.endpoints || [];
    document.getElementById('endpointRows').innerHTML = endpoints.length ? endpoints.map(endpoint => {
        const bad = Number(endpoint.p95Ms || 0) > threshold
            || Number(endpoint.errorRate || 0) > Number(api.errorRateThreshold || 0.01);
        return '<tr><td><strong>' + escapeHtml(endpoint.endpoint) + '</strong></td><td>' + endpoint.count
            + '</td><td>' + endpoint.p95Ms + ' ms</td><td>' + endpoint.maxMs + ' ms</td><td>'
            + percent(Number(endpoint.errorRate || 0) * 100) + '</td><td><span class="status-text '
            + (bad ? 'bad' : 'good') + '">' + (bad ? '优先优化' : '正常') + '</span></td></tr>';
    }).join('') : '<tr><td colspan="6" class="empty-cell">等待请求样本...</td></tr>';
}

document.getElementById('refreshBtn').addEventListener('click', loadDecisionDashboard);
document.getElementById('logoutBtn').addEventListener('click', async function () {
    if (await AppDialog.confirm('确定要退出吗？', '退出后台')) {
        localStorage.removeItem('admin');
        localStorage.removeItem('token');
        location.href = '/pages/admin/admin-login.html';
    }
});

if (checkAdminLogin()) {
    loadDecisionDashboard();
    setInterval(loadDecisionDashboard, 30000);
}
