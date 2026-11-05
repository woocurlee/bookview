// 책장(읽은 책) 캘린더 + 추가/수정/삭제 로직

let bsReading = [];      // 읽는 중 (finishedAt == null)
let bsFinished = [];     // 완독 (finishedAt != null)
let bsFinishedByDate = {}; // 'YYYY-MM-DD' -> [entry, ...]
let bsIsOwner = false;

let calYear = 0;         // 현재 표시 중인 달력 연/월(0-based)
let calMonth = 0;

let bsSelectedBook = null;   // 추가 모달에서 선택한 책
let bsEditingEntry = null;   // 상세/편집 모달에서 다루는 항목

let bsCurrentView = 'calendar'; // 'calendar' | 'shelf'
let bsResizeTimer = null;
const BS_VIEW_KEY = 'bookview.shelfView';

document.addEventListener('DOMContentLoaded', () => {
    const panel = document.getElementById('panelShelf');
    if (!panel) return;
    bsIsOwner = panel.dataset.owner === 'true';

    bsReading = parseDataIsland('bookshelfReadingData');
    bsFinished = parseDataIsland('bookshelfFinishedData');

    bsFinishedByDate = {};
    bsFinished.forEach(e => {
        if (!e.finishedAt) return;
        (bsFinishedByDate[e.finishedAt] = bsFinishedByDate[e.finishedAt] || []).push(e);
    });

    // 달력 시작 위치: 가장 최근 완독 달, 없으면 이번 달
    const anchor = bsFinished.length > 0 && bsFinished[0].finishedAt
        ? new Date(bsFinished[0].finishedAt)
        : new Date();
    calYear = anchor.getFullYear();
    calMonth = anchor.getMonth();

    Modal.setupOutsideClick('addBookModal');
    Modal.setupOutsideClick('entryDetailModal');
    Modal.setupOutsideClick('dayBooksModal');

    renderShelf();
    window.addEventListener('resize', bsOnResize);
    bindShelfControls();

    // 저장/수정/삭제 후 리로드해도 '읽은 책' 탭에 머물도록 복원
    if (location.hash === '#shelf') switchBookshelfTab('shelf');
});

function bindShelfControls() {
    const search = document.getElementById('bsBookSearch');
    if (search) {
        search.addEventListener('keydown', (event) => {
            if (event.key !== 'Enter') return;
            event.preventDefault();
            bsSearchBooks();
        });
    }

    document.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-action]');
        if (!trigger) return;

        switch (trigger.dataset.action) {
            case 'bs-tab':
                switchBookshelfTab(trigger.dataset.tab);
                break;
            case 'bs-view':
                setShelfView(trigger.dataset.view);
                break;
            case 'bs-month':
                bookshelfChangeMonth(Number(trigger.dataset.delta));
                break;
            case 'bs-add-open':
                openAddBookModal();
                break;
            case 'bs-add-close':
                closeAddBookModal();
                break;
            case 'bs-search':
                bsSearchBooks();
                break;
            case 'bs-clear-selection':
                bsClearSelection();
                break;
            case 'bs-submit':
                bsSubmitEntry();
                break;
            case 'bs-detail-close':
                closeEntryDetailModal();
                break;
            case 'bs-entry-delete':
                bsDeleteEntry();
                break;
            case 'bs-entry-save':
                bsSaveEntryEdit();
                break;
            case 'bs-day-close':
                closeDayModal();
                break;
        }
    });
}

function bsOnResize() {
    clearTimeout(bsResizeTimer);
    bsResizeTimer = setTimeout(() => {
        if (bsCurrentView === 'shelf') renderShelfView();
    }, 200);
}

// 같은 책(bookIsbn)의 재독 순번. renderShelf/bsRerender에서 매번 다시 계산한다.
// - orderMap: 완독 항목 id -> 완독일 오름차순 회차(1부터)
// - countByIsbn: isbn -> 완독 총 횟수 (읽는 중 항목의 "N번째 읽는 중" 안내에 사용)
let bsReadOrder = { orderMap: {}, countByIsbn: {} };

function buildReadOrderMaps() {
    const byIsbn = {};
    bsFinished.forEach(e => {
        if (!e.bookIsbn) return;
        (byIsbn[e.bookIsbn] = byIsbn[e.bookIsbn] || []).push(e);
    });
    const orderMap = {};
    const countByIsbn = {};
    Object.keys(byIsbn).forEach(isbn => {
        const list = byIsbn[isbn].slice().sort((a, b) => (a.finishedAt || '').localeCompare(b.finishedAt || ''));
        countByIsbn[isbn] = list.length;
        list.forEach((e, idx) => { orderMap[e.id] = idx + 1; });
    });
    bsReadOrder = { orderMap, countByIsbn };
}

// 책장/모달 등에 노출하는 정식 문구. 1회차(재독 아님)는 노출할 정보가 없으므로 null.
// - 완독 항목: "2회차 읽음"
// - 읽는 중 항목: 같은 책의 완독 이력이 있으면 "2번째 읽는 중" (확정 회차 아님)
function readTag(entry) {
    if (!entry.bookIsbn) return null;
    if (entry.finishedAt) {
        const n = bsReadOrder.orderMap[entry.id];
        return (n && n >= 2) ? `${n}회차 읽음` : null;
    }
    const total = bsReadOrder.countByIsbn[entry.bookIsbn] || 0;
    return total >= 1 ? `${total + 1}번째 읽는 중` : null;
}

// 책등/정면책처럼 공간이 좁은 곳에 쓰는 축약형 ("×2" / "재독")
function readTagCompact(entry) {
    if (!entry.bookIsbn) return null;
    if (entry.finishedAt) {
        const n = bsReadOrder.orderMap[entry.id];
        return (n && n >= 2) ? `×${n}` : null;
    }
    const total = bsReadOrder.countByIsbn[entry.bookIsbn] || 0;
    return total >= 1 ? '재독' : null;
}

// 서버 응답(BookshelfEntry) → 클라이언트 표시용 항목
function toClientEntry(e) {
    return {
        id: e.id,
        bookTitle: e.bookTitle,
        bookAuthor: e.bookAuthor,
        bookThumbnail: e.bookThumbnail,
        bookIsbn: e.bookIsbn,
        startedAt: e.startedAt,
        finishedAt: e.finishedAt,
    };
}

// 완독=완독일 최신순, 읽는 중=시작일 최신순 (서버 정렬과 동일하게 유지)
function bsSortEntries() {
    bsFinished.sort((a, b) => (b.finishedAt || '').localeCompare(a.finishedAt || ''));
    bsReading.sort((a, b) => (b.startedAt || '').localeCompare(a.startedAt || ''));
}

// 새로고침 없이 현재 뷰를 다시 그린다 (추가/수정/삭제 후)
function bsRerender() {
    bsFinishedByDate = {};
    bsFinished.forEach(e => {
        if (!e.finishedAt) return;
        (bsFinishedByDate[e.finishedAt] = bsFinishedByDate[e.finishedAt] || []).push(e);
    });
    buildReadOrderMaps();

    const total = bsReading.length + bsFinished.length;
    const empty = document.getElementById('shelfEmpty');
    const viewCalendar = document.getElementById('viewCalendar');
    const viewShelf = document.getElementById('viewShelf');
    const seg = document.querySelector('#panelShelf .bs-seg');

    if (total === 0) {
        empty.classList.remove('hidden');
        viewCalendar.classList.add('hidden');
        viewShelf.classList.add('hidden');
        if (seg) seg.classList.add('hidden');
        return;
    }
    empty.classList.add('hidden');
    if (seg) seg.classList.remove('hidden');
    document.querySelectorAll('#panelShelf .bs-seg-btn').forEach(b => {
        b.classList.toggle('on', b.dataset.view === bsCurrentView);
    });

    renderReadingStrip();
    renderCalendar();
    const isShelf = bsCurrentView === 'shelf';
    viewCalendar.classList.toggle('hidden', isShelf);
    viewShelf.classList.toggle('hidden', !isShelf);
    if (isShelf) renderShelfView();
}

function parseDataIsland(id) {
    const el = document.getElementById(id);
    if (!el) return [];
    try {
        return JSON.parse(el.textContent) || [];
    } catch {
        return [];
    }
}

// ── 탭 전환 ────────────────────────────────────────────────
function switchBookshelfTab(tab) {
    const isReviews = tab === 'reviews';
    // 패널 표시는 루트 클래스(인라인 CSS)로 제어 → Tailwind 비동기 로딩과 무관하게 즉시 반영
    document.documentElement.classList.toggle('__view-shelf', !isReviews);

    setTabActive('tabReviewsBtn', isReviews);
    setTabActive('tabShelfBtn', !isReviews);

    // 현재 탭을 해시에 기록 (새로고침 시 유지)
    history.replaceState(null, '', isReviews ? '#reviews' : '#shelf');

    // 책장 뷰가 활성 상태로 패널이 보이면 (너비 확보) 다시 렌더
    if (!isReviews && bsCurrentView === 'shelf') renderShelfView();
}

function setTabActive(btnId, active) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.classList.toggle('text-stone-800', active);
    btn.classList.toggle('border-amber-500', active);
    btn.classList.toggle('text-stone-400', !active);
    btn.classList.toggle('border-transparent', !active);
}

// ── 렌더링 ────────────────────────────────────────────────
function renderShelf() {
    buildReadOrderMaps();
    const total = bsReading.length + bsFinished.length;
    const empty = document.getElementById('shelfEmpty');
    const viewCalendar = document.getElementById('viewCalendar');
    const viewShelf = document.getElementById('viewShelf');
    const seg = document.querySelector('#panelShelf .bs-seg');

    if (total === 0) {
        empty.classList.remove('hidden');
        viewCalendar.classList.add('hidden');
        viewShelf.classList.add('hidden');
        if (seg) seg.classList.add('hidden');
        return;
    }
    empty.classList.add('hidden');
    if (seg) seg.classList.remove('hidden');

    renderReadingStrip();
    renderCalendar();

    let saved = null;
    try { saved = localStorage.getItem(BS_VIEW_KEY); } catch (e) { /* localStorage 불가 시 기본값 */ }
    setShelfView(saved === 'shelf' ? 'shelf' : 'calendar');
}

// 달력/책장 뷰 전환 (마지막 선택은 localStorage에 저장)
function setShelfView(view) {
    bsCurrentView = view === 'shelf' ? 'shelf' : 'calendar';
    const isShelf = bsCurrentView === 'shelf';
    document.getElementById('viewCalendar').classList.toggle('hidden', isShelf);
    document.getElementById('viewShelf').classList.toggle('hidden', !isShelf);
    document.querySelectorAll('#panelShelf .bs-seg-btn').forEach(b => {
        b.classList.toggle('on', b.dataset.view === bsCurrentView);
    });
    try { localStorage.setItem(BS_VIEW_KEY, bsCurrentView); } catch (e) { /* 무시 */ }
    if (isShelf) renderShelfView();
}

// ── 책 시각 속성 (책 id 시드로 고정: 새로고침해도 안 변함) ──
const BS_FILLS = ['f-terra', 'f-ochre', 'f-sage', 'f-teal', 'f-cream', 'f-clay', 'f-plum', 'f-denim', 'f-olive', 'f-rust'];
const BS_WIDTHS = ['k-s', 'k-m', 'k-l', 'k-xl'];
const BS_HEIGHTS = ['h-a', 'h-b', 'h-c', 'h-d'];
const BS_MOTIFS = ['d-band', 'd-dot', 'd-tick', ''];
const BS_WIDTH_PX = { 'k-s': 26, 'k-m': 34, 'k-l': 43, 'k-xl': 52 };

function bsHash(str) {
    let h = 2166136261;
    for (let i = 0; i < str.length; i++) {
        h ^= str.charCodeAt(i);
        h = Math.imul(h, 16777619);
    }
    return h >>> 0;
}

function bookStyle(entry) {
    const key = String(entry.id || entry.bookIsbn || entry.bookTitle || '');
    const h = bsHash(key);
    return {
        fill: BS_FILLS[h % BS_FILLS.length],
        width: BS_WIDTHS[(h >>> 3) % BS_WIDTHS.length],
        height: BS_HEIGHTS[(h >>> 7) % BS_HEIGHTS.length],
        motif: BS_MOTIFS[(h >>> 11) % BS_MOTIFS.length],
    };
}

// ── 읽는 중 스트립 (미니 정면 책) ──
function renderReadingStrip() {
    const strip = document.getElementById('readingStrip');
    const list = document.getElementById('readingStripList');
    if (bsReading.length === 0) {
        strip.classList.add('hidden');
        return;
    }
    strip.classList.remove('hidden');
    list.innerHTML = '';
    bsReading.forEach(entry => {
        const el = document.createElement('button');
        el.type = 'button';
        el.className = 'bs-mini-faced ' + bookStyle(entry).fill;
        const tag = readTag(entry);
        el.title = tag ? `${entry.bookTitle} · ${tag}` : entry.bookTitle; // 터치 기기는 title 툴팁이 안 뜨므로, 재독 정보는 상세 모달에서도 확인 가능
        el.onclick = () => openEntryDetail(entry);
        el.innerHTML = `<span class="mk"></span><span class="mm">${escapeHtml(entry.bookTitle)}</span>`;
        list.appendChild(el);
    });
}

// ── 달력 ──
function renderCalendar() {
    document.getElementById('calendarLabel').textContent = `${calYear}년 ${calMonth + 1}월`;
    const grid = document.getElementById('calendarGrid');
    grid.innerHTML = '';

    const firstWeekday = new Date(calYear, calMonth, 1).getDay(); // 0=일
    const daysInMonth = new Date(calYear, calMonth + 1, 0).getDate();

    for (let i = 0; i < firstWeekday; i++) grid.appendChild(emptyCell());
    for (let day = 1; day <= daysInMonth; day++) {
        const dateKey = `${calYear}-${pad(calMonth + 1)}-${pad(day)}`;
        grid.appendChild(dayCell(day, bsFinishedByDate[dateKey] || []));
    }
}

function emptyCell() {
    const cell = document.createElement('div');
    cell.className = 'bs-cell empty';
    return cell;
}

function dayCell(day, entries) {
    const cell = document.createElement('div');
    cell.className = 'bs-cell';

    const dn = document.createElement('span');
    dn.className = 'dn';
    dn.textContent = day;
    cell.appendChild(dn);

    if (entries.length > 0) {
        cell.classList.add('has');
        cell.tabIndex = 0;
        const soloTag = entries.length === 1 ? readTag(entries[0]) : null;
        cell.title = entries.length > 1
            ? `${entries.length}권 완독`
            : (soloTag ? `${entries[0].bookTitle} · ${soloTag}` : entries[0].bookTitle);

        const mini = document.createElement('span');
        mini.className = 'mini ' + bookStyle(entries[0]).fill;
        if (entries.length > 1) {
            const plus = document.createElement('span');
            plus.className = 'plus';
            plus.textContent = '+' + (entries.length - 1);
            mini.appendChild(plus); // 책 모서리에 부착 (날짜 숫자와 충돌 방지)
        }
        cell.appendChild(mini);

        const bt = document.createElement('span');
        bt.className = 'bt';
        bt.textContent = entries[0].bookTitle;
        cell.appendChild(bt);

        cell.onclick = entries.length > 1 ? () => openDayModal(entries) : () => openEntryDetail(entries[0]);
    }
    return cell;
}

// ── 책장 뷰 (연도별 그룹, 여러 단으로 wrap) ──
function renderShelfView() {
    const host = document.getElementById('viewShelf');
    const scene = document.getElementById('bsScene');
    if (!host || host.classList.contains('hidden')) return;
    const innerW = scene.clientWidth - 40; // scene 좌우 패딩(20*2)
    if (innerW <= 0) return; // 아직 화면에 없음 (탭 전환 시 다시 렌더)
    host.innerHTML = '';

    if (bsReading.length > 0) {
        renderShelfSection(host, { label: '읽는 중', now: true, count: bsReading.length },
            bsReading, { allFaced: true, plant: true }, innerW);
    }

    const byYear = {};
    bsFinished.forEach(e => {
        const y = (e.finishedAt || '').slice(0, 4) || '기타';
        (byYear[y] = byYear[y] || []).push(e);
    });
    Object.keys(byYear).sort((a, b) => b.localeCompare(a)).forEach(year => {
        const list = byYear[year];
        renderShelfSection(host, { label: year, count: list.length },
            list, { facedId: list[0] && list[0].id }, innerW);
    });
}

// 한 그룹을 라벨 + 여러 선반 줄로 렌더 (책 너비 합으로 줄바꿈)
function renderShelfSection(host, meta, entries, opts, innerW) {
    const wrap = document.createElement('div');
    wrap.className = 'bs-shelf';

    const label = document.createElement('span');
    label.className = 'bs-shelf-label' + (meta.now ? ' now' : '');
    label.innerHTML = `${escapeHtml(meta.label)} <span class="n">${meta.now ? meta.count + '권' : '· ' + meta.count}</span>`;
    wrap.appendChild(label);

    const items = entries.map(e => {
        const faced = Boolean(opts.allFaced || (opts.facedId && e.id === opts.facedId));
        const w = (faced ? 74 : (BS_WIDTH_PX[bookStyle(e).width] || 34)) + 4; // +border
        return { entry: e, faced, w };
    });
    if (opts.plant) items.push({ plant: true, w: 54 });

    const gap = 7;
    let row = [];
    let rowW = 0;
    const flush = () => {
        if (!row.length) return;
        const books = document.createElement('div');
        books.className = 'bs-books';
        row.forEach(it => {
            books.appendChild(it.plant ? makePlant() : (it.faced ? makeFaced(it.entry) : makeSpine(it.entry)));
        });
        wrap.appendChild(books);
        const plank = document.createElement('div');
        plank.className = 'bs-plank';
        wrap.appendChild(plank);
        row = [];
        rowW = 0;
    };
    items.forEach(it => {
        const w = it.w + gap;
        if (rowW + w > innerW && row.length) flush();
        row.push(it);
        rowW += w;
    });
    flush();

    host.appendChild(wrap);
}

function makeSpine(entry) {
    const s = bookStyle(entry);
    const el = document.createElement('button');
    el.type = 'button';
    el.className = ['bs-book', s.width, s.height, s.fill, s.motif].filter(Boolean).join(' ');
    const tagFull = readTag(entry);
    el.title = tagFull ? `${entry.bookTitle} · ${tagFull}` : entry.bookTitle;
    if (s.width === 'k-m' || s.width === 'k-l' || s.width === 'k-xl') {
        el.innerHTML = `<span class="cap"><span>${escapeHtml(entry.bookTitle)}</span></span>`;
    }
    // 폭이 가장 좁은 k-s는 배지를 얹을 공간이 없어 생략
    if (s.width !== 'k-s') {
        const tag = readTagCompact(entry);
        if (tag) el.insertAdjacentHTML('beforeend', `<span class="reread">${escapeHtml(tag)}</span>`);
    }
    el.onclick = () => openEntryDetail(entry);
    return el;
}

function makeFaced(entry) {
    const el = document.createElement('button');
    el.type = 'button';
    el.className = 'bs-bkf ' + bookStyle(entry).fill;
    const tagFull = readTag(entry);
    el.title = tagFull ? `${entry.bookTitle} · ${tagFull}` : entry.bookTitle;
    const mark = entry.finishedAt ? '' : '<span class="mark"></span>';
    const tag = readTagCompact(entry);
    const badge = tag ? `<span class="reread">${escapeHtml(tag)}</span>` : '';
    el.innerHTML = `${mark}${badge}<span class="ct">${escapeHtml(entry.bookTitle)}</span><span class="ca">${escapeHtml(entry.bookAuthor)}</span>`;
    el.onclick = () => openEntryDetail(entry);
    return el;
}

function makePlant() {
    const el = document.createElement('div');
    el.className = 'bs-plant';
    el.setAttribute('aria-hidden', 'true');
    el.innerHTML = '<span class="leaf l3"></span><span class="leaf l1"></span><span class="leaf l2"></span><span class="pot"></span>';
    return el;
}

// 같은 날 완독한 여러 책 목록 모달
function openDayModal(entries) {
    const list = document.getElementById('dayBooksList');
    const finishedAt = entries[0] && entries[0].finishedAt;
    document.getElementById('dayModalTitle').textContent =
        `${finishedAt ? shortDate(finishedAt) + ' ' : ''}완독한 책 ${entries.length}권`;
    list.innerHTML = '';
    entries.forEach(entry => {
        const row = document.createElement('button');
        row.type = 'button';
        row.className = 'flex gap-3 items-center p-2 rounded-lg hover:bg-stone-50 text-left w-full';
        row.onclick = () => { closeDayModal(); openEntryDetail(entry); };
        const tag = readTag(entry);
        row.innerHTML = `
            <div class="w-10 h-14 flex-shrink-0 rounded overflow-hidden bg-stone-100">
                ${thumbHtml(entry, 'w-full h-full object-cover')}
            </div>
            <div class="min-w-0">
                <div class="font-semibold text-sm text-stone-800 line-clamp-1">${escapeHtml(entry.bookTitle)}</div>
                <div class="text-xs text-stone-500 line-clamp-1">${escapeHtml(entry.bookAuthor)}</div>
                ${tag ? `<div class="text-xs text-amber-600 font-bold mt-0.5">🔁 ${escapeHtml(tag)}</div>` : ''}
            </div>`;
        list.appendChild(row);
    });
    Modal.open('dayBooksModal');
}

function closeDayModal() {
    Modal.close('dayBooksModal');
}

function bookshelfChangeMonth(delta) {
    calMonth += delta;
    if (calMonth < 0) { calMonth = 11; calYear--; }
    else if (calMonth > 11) { calMonth = 0; calYear++; }
    renderCalendar();
}

// ── 항목 상세/편집 ─────────────────────────────────────────
function openEntryDetail(entry) {
    bsEditingEntry = entry;
    document.getElementById('edTitle').textContent = entry.bookTitle;
    document.getElementById('edAuthor').textContent = entry.bookAuthor;
    setThumbnail('edThumbnail', entry);

    const tagEl = document.getElementById('edReadTag');
    const tag = readTag(entry);
    tagEl.textContent = tag ? `🔁 ${tag}` : '';
    tagEl.classList.toggle('hidden', !tag);

    const ownerControls = document.getElementById('edOwnerControls');
    const readonly = document.getElementById('edReadonly');

    if (bsIsOwner) {
        readonly.classList.add('hidden');
        ownerControls.classList.remove('hidden');
        document.getElementById('edStartedAt').value = entry.startedAt || '';
        document.getElementById('edFinishedAt').value = entry.finishedAt || '';
    } else {
        ownerControls.classList.add('hidden');
        readonly.classList.remove('hidden');
        const started = entry.startedAt ? shortDate(entry.startedAt) : '―';
        const finished = entry.finishedAt ? shortDate(entry.finishedAt) : '읽는 중';
        readonly.innerHTML = `시작 ${started} · 완독 ${finished}`;
    }
    Modal.open('entryDetailModal');
}

function closeEntryDetailModal() {
    Modal.close('entryDetailModal');
    bsEditingEntry = null;
}

async function bsSaveEntryEdit() {
    if (!bsEditingEntry) return;
    const startedAt = document.getElementById('edStartedAt').value || null;
    const finishedAt = document.getElementById('edFinishedAt').value || null;
    if (startedAt && finishedAt && finishedAt < startedAt) {
        Alert.error('완독일은 시작일보다 앞설 수 없습니다.');
        return;
    }
    const editDupMsg = duplicateReason(bsEditingEntry.bookIsbn, finishedAt, bsEditingEntry.id);
    if (editDupMsg) {
        Alert.error(editDupMsg);
        return;
    }
    try {
        const updated = await API.put(`/api/bookshelf/${bsEditingEntry.id}`, { startedAt, finishedAt });
        const entry = toClientEntry(updated);
        bsReading = bsReading.filter(e => e.id !== entry.id);
        bsFinished = bsFinished.filter(e => e.id !== entry.id);
        if (entry.finishedAt) bsFinished.push(entry); else bsReading.push(entry);
        bsSortEntries();
        closeEntryDetailModal();
        bsRerender();
    } catch (e) {
        Alert.error(e.message);
    }
}

async function bsDeleteEntry() {
    if (!bsEditingEntry) return;
    if (!Alert.confirm('이 책을 책장에서 삭제할까요?')) return;
    try {
        const id = bsEditingEntry.id;
        await API.delete(`/api/bookshelf/${id}`);
        bsReading = bsReading.filter(e => e.id !== id);
        bsFinished = bsFinished.filter(e => e.id !== id);
        closeEntryDetailModal();
        bsRerender();
    } catch (e) {
        Alert.error(e.message);
    }
}

// ── 책 추가 모달 ───────────────────────────────────────────
function openAddBookModal() {
    bsClearSelection();
    document.getElementById('bsBookList').innerHTML = '';
    document.getElementById('bsBookSearch').value = '';
    Modal.open('addBookModal');
    setTimeout(() => document.getElementById('bsBookSearch').focus(), 50);
}

function closeAddBookModal() {
    Modal.close('addBookModal');
}

async function bsSearchBooks() {
    const query = document.getElementById('bsBookSearch').value.trim();
    if (!query) {
        Alert.error('검색어를 입력하세요');
        return;
    }
    const list = document.getElementById('bsBookList');
    const loading = document.getElementById('bsBookLoading');
    list.innerHTML = '';
    loading.classList.remove('hidden');

    try {
        const data = await API.get(`/api/external/books/search?query=${encodeURIComponent(query)}&page=1&size=12`);
        loading.classList.add('hidden');
        const docs = (data && data.documents) || [];
        if (docs.length === 0) {
            list.innerHTML = '<p class="text-stone-500 col-span-full text-center py-4">검색 결과가 없습니다.</p>';
            return;
        }
        docs.forEach(book => {
            const item = document.createElement('div');
            item.className = 'flex flex-col p-3 border-2 border-stone-200 rounded-lg cursor-pointer transition-all hover:border-amber-500 hover:bg-amber-50';
            item.onclick = () => bsSelectBook(book);
            item.innerHTML = `
                ${coverImgTag(book.thumbnail, 'w-full h-40 object-contain rounded mb-2', book.title)}
                <div class="font-semibold text-sm mb-0.5 line-clamp-2">${escapeHtml(book.title)}</div>
                <div class="text-stone-500 text-xs line-clamp-1">${escapeHtml((book.authors || []).join(', '))}</div>
            `;
            list.appendChild(item);
        });
    } catch (e) {
        loading.classList.add('hidden');
        list.innerHTML = '<p class="text-red-500 col-span-full text-center py-4">검색에 실패했습니다.</p>';
    }
}

function bsSelectBook(book) {
    bsSelectedBook = {
        bookTitle: book.title,
        bookAuthor: (book.authors || []).join(', '),
        bookIsbn: book.isbn || '',
        bookThumbnail: book.thumbnail || null,
    };
    document.getElementById('bsSelectedTitle').textContent = bsSelectedBook.bookTitle;
    document.getElementById('bsSelectedAuthor').textContent = bsSelectedBook.bookAuthor;
    document.getElementById('bsSelectedThumbnail').src = bsSelectedBook.bookThumbnail || '';
    document.getElementById('bsSearchSection').classList.add('hidden');
    document.getElementById('bsSelectedSection').classList.remove('hidden');
}

function bsClearSelection() {
    bsSelectedBook = null;
    document.getElementById('bsStartedAt').value = '';
    document.getElementById('bsFinishedAt').value = '';
    document.getElementById('bsSelectedSection').classList.add('hidden');
    document.getElementById('bsSearchSection').classList.remove('hidden');
}

async function bsSubmitEntry() {
    if (!bsSelectedBook) return;
    const startedAt = document.getElementById('bsStartedAt').value || null;
    const finishedAt = document.getElementById('bsFinishedAt').value || null;
    if (startedAt && finishedAt && finishedAt < startedAt) {
        Alert.error('완독일은 시작일보다 앞설 수 없습니다.');
        return;
    }
    const addDupMsg = duplicateReason(bsSelectedBook.bookIsbn, finishedAt, null);
    if (addDupMsg) {
        Alert.error(addDupMsg);
        return;
    }
    try {
        const created = await API.post('/api/bookshelf', { ...bsSelectedBook, startedAt, finishedAt });
        const entry = toClientEntry(created);
        if (entry.finishedAt) {
            bsFinished.push(entry);
            const d = new Date(entry.finishedAt); // 완독 등록 시 해당 달로 달력 이동
            calYear = d.getFullYear();
            calMonth = d.getMonth();
        } else {
            bsReading.push(entry);
        }
        bsSortEntries();
        closeAddBookModal();
        bsRerender();
    } catch (e) {
        Alert.error(e.message);
    }
}

// ── 헬퍼 ─────────────────────────────────────────────────
function setThumbnail(imgId, entry) {
    const img = document.getElementById(imgId);
    if (entry.bookThumbnail) {
        img.src = hiResCover(entry.bookThumbnail);
        img.onerror = function () { img.onerror = null; img.src = entry.bookThumbnail; }; // 폴백
        img.classList.remove('hidden');
    } else {
        img.removeAttribute('src');
        img.classList.add('hidden');
    }
}

function thumbHtml(entry, imgClass) {
    if (entry.bookThumbnail) {
        return coverImgTag(entry.bookThumbnail, imgClass, entry.bookTitle);
    }
    return `<div class="w-full h-full flex items-center justify-center bg-stone-200 text-stone-400 text-lg">📖</div>`;
}

// 중복 등록 사유 반환 (없으면 null). ISBN 없으면 검사 안 함
// - 읽는 중: 같은 책이 이미 읽는 중이면 차단
// - 완독: 같은 책을 같은 완독일로 등록했으면 차단(다른 날짜는 재독 허용)
function duplicateReason(bookIsbn, finishedAt, excludeId) {
    if (!bookIsbn) return null;
    if (!finishedAt) {
        const dup = bsReading.some(e => e.id !== excludeId && e.bookIsbn === bookIsbn);
        return dup ? "이미 '읽는 중'으로 등록된 책입니다." : null;
    }
    const dup = bsFinished.some(e =>
        e.id !== excludeId && e.bookIsbn === bookIsbn && e.finishedAt === finishedAt);
    return dup ? '이미 같은 날짜에 완독한 책으로 등록되어 있습니다.' : null;
}

function pad(n) {
    return String(n).padStart(2, '0');
}

function shortDate(isoDate) {
    // 'YYYY-MM-DD' -> 'YYYY.MM.DD'
    return isoDate.replace(/-/g, '.');
}

function escapeHtml(str) {
    return String(str == null ? '' : str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
