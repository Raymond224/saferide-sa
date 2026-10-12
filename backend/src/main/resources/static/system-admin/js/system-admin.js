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
