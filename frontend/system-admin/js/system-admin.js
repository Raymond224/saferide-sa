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

function openAddUserForm(){
  document.getElementById('addUserModal').style.display = 'flex';
}

function closeAddUserForm(){
  document.getElementById('addUserModal').style.display = 'none';

  document.getElementById('newUserName').value = '';
  document.getElementById('newUserEmail').value = '';
  document.getElementById('newUserRole').value = '';
}

function addUser(){
  const name = document.getElementById('newUserName').value.trim();
  const email = document.getElementById('newUserEmail').value.trim();
  const role = document.getElementById('newUserRole').value;

  if(!name || !email || !role){
    alert('Please complete all fields.');
    return;
  }

  const table = document.getElementById('userTable');
  const row = document.createElement('tr');

  row.dataset.role = role;
  row.dataset.status = 'Active';

  row.innerHTML = `
    <td>${name}</td>
    <td>${email}</td>
    <td>${role}</td>
    <td><span class="badge badge-green">Active</span></td>
    <td>${new Date().toISOString().split('T')[0]}</td>
    <td>
      <button class="btn btn-outline btn-sm" onclick="toggleStatus(this)">
        Suspend
      </button>
    </td>
  `;

  table.appendChild(row);

  closeAddUserForm();

  alert('User added successfully.');
}
