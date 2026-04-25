import axios from 'axios';
const api = axios.create({ baseURL: '/api' });
export const getCurrentMap = async () => {
    const res = await api.get('/map/current');
    return res.data;
};
export const createNode = async (mapId, parentId, text) => {
    await api.post('/nodes/create', { mapId, parentId, text, position: 'child' });
};
export const editNode = async (mapId, nodeId, text) => {
    await api.post('/nodes/edit', { mapId, nodeId, text });
};
export const deleteNode = async (mapId, nodeId) => {
    await api.post('/nodes/delete', { mapId, nodeId });
};
export const toggleFold = async (mapId, nodeId, folded) => {
    await api.post('/nodes/toggle-fold', { mapId, nodeId, folded });
};
