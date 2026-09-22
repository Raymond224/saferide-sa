function filterUsers(){
  const q=document.getElementById('userSearch')?.value.toLowerCase()||'';
  const role=document.getElementById('roleFilter')?.value||'';
  const status=document.getElementById('statusFilter')?.value||'';
  document.querySelectorAll('#userTable tr').forEach(row=>{
    const text=row.innerText.toLowerCase();
    row.style.display=(!q||text.includes(q))&&(!role||row.dataset.role===role)&&(!status||row.dataset.status===status)?'':'none';
  });
}
function toggleStatus(btn){
  const row=btn.closest('tr');
  const badge=row.querySelector('.badge');
  const suspended=row.dataset.status==='Suspended';
  row.dataset.status=suspended?'Active':'Suspended';
  badge.textContent=suspended?'Active':'Suspended';
  badge.className='badge '+(suspended?'badge-green':'badge-red');
  btn.textContent=suspended?'Suspend':'Activate';
}
function filterSchools(){
  const q=document.getElementById('schoolSearch')?.value.toLowerCase()||'';
  document.querySelectorAll('#schoolTable tr').forEach(row=>{
    row.style.display=row.innerText.toLowerCase().includes(q)?'':'none';
  });
}
function filterAudit(){
  const q=document.getElementById('auditSearch')?.value.toLowerCase()||'';
  const action=document.getElementById('auditAction')?.value||'';
  document.querySelectorAll('#auditTable tr').forEach(row=>{
    const text=row.innerText.toLowerCase();
    const rowAction=row.children[2]?.innerText||'';
    row.style.display=(!q||text.includes(q))&&(!action||rowAction===action)?'':'none';
  });
}

function viewAuditLog(button){
  const row = button.closest('tr');
  const cells = row.querySelectorAll('td');

  document.getElementById('viewAuditTimestamp').textContent = cells[0].textContent;
  document.getElementById('viewAuditUser').textContent = cells[1].textContent;
  document.getElementById('viewAuditAction').textContent = cells[2].textContent;
  document.getElementById('viewAuditTable').textContent = cells[3].textContent;
  document.getElementById('viewAuditRecord').textContent = cells[4].textContent;
  document.getElementById('viewAuditIP').textContent = cells[5].textContent;

  document.getElementById('auditDetailsModal').style.display = 'flex';
}

function closeAuditDetails(){
  document.getElementById('auditDetailsModal').style.display = 'none';
}