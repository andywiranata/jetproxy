import React, { useState } from 'react';
import { User, Pencil, Trash2, Plus } from 'lucide-react';
import type { Config, User as UserType } from '../types/proxy';
import { YamlPopupEditor } from '../components/YamlPopupEditor';

interface UsersPageProps {
  config: Config;
  onConfigUpdate: (config: Config) => void;
}

const userTemplate = {
  username: "new_user",
  password: "",
  role: "user"
};

export function UsersPage({ config, onConfigUpdate }: UsersPageProps) {
  const [selectedUser, setSelectedUser] = useState<UserType | null>(null);
  const [showUserEditor, setShowUserEditor] = useState(false);

  const handleEditUser = (user: UserType) => {
    if (user.role === 'administrator') return;
    setSelectedUser(user);
    setShowUserEditor(true);
  };

  const handleDeleteUser = (username: string) => {
    const user = config.users.find(u => u.username === username);
    if (user?.role === 'administrator') return;

    // onConfigUpdate({
    //   ...config,
    //   users: config.users.filter(u => u.username !== username)
    // });
  };

  const handleUserSave = (user: UserType) => {
    
    setSelectedUser(null);
    setShowUserEditor(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold">Users</h2>
        {/* <button
          onClick={() => setShowUserEditor(true)}
          className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add User
        </button> */}
      </div>

      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role</th>
              {/* <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th> */}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {config.users.map((user) => (
              <tr key={user.username} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center">
                    <User className="h-5 w-5 text-gray-400 mr-3" />
                    <div className="text-sm font-medium text-gray-900">{user.username}</div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    user.role === 'administrator' 
                      ? 'bg-purple-100 text-purple-800' 
                      : 'bg-green-100 text-green-800'
                  }`}>
                    {user.role}
                  </span>
                </td>
                {/* <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => handleEditUser(user)}
                    disabled={user.role === 'administrator'}
                    className={`text-blue-600 hover:text-blue-900 mr-3 ${
                      user.role === 'administrator' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteUser(user.username)}
                    disabled={user.role === 'administrator'}
                    className={`text-red-600 hover:text-red-900 ${
                      user.role === 'administrator' ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </td> */}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* {showUserEditor && (
        <YamlPopupEditor
          title={selectedUser ? "Edit User" : "Add User"}
          initialValue={selectedUser}
          template={userTemplate}
          onSave={handleUserSave}
          onClose={() => {
            setShowUserEditor(false);
            setSelectedUser(null);
          }}
        />
      )} */}
    </div>
  );
}