const { MenuItem } = require('../models');

const mapPublicMenuItem = (item) => {
  const data = item.toJSON();
  const isActive = data.menuStatus === 'Active';

  if (data.menuType === 'top') {
    return {
      topmenuId: data.id,
      topmenuName: data.menuName,
      topmenuStatusId: data.menuStatusId,
      topmenuStatus: data.menuStatus,
      topmenuActiveIcon: data.menuActiveIcon,
      topmenuInActiveIcon: data.menuInActiveIcon,
      isActive: isActive, // Crucial for Android filtering
      id: data.id,
      name: data.menuName,
      active: isActive
    };
  }

  return {
    bottommenuId: data.id,
    bottommenuName: data.menuName,
    bottommenuStatusId: data.menuStatusId,
    bottommenuStatus: data.menuStatus,
    bottommenuActiveIcon: data.menuActiveIcon,
    bottommenuInActiveIcon: data.menuInActiveIcon,
    isActive: isActive, // Crucial for Android filtering
    id: data.id,
    name: data.menuName,
    active: isActive
  };
};

exports.getMenu = async (req, res, next) => {
  try {
    const items = await MenuItem.findAll({
      order: [['menuType', 'ASC'], ['sortOrder', 'ASC'], ['id', 'ASC']]
    });

    const topMenu = items.filter((item) => item.menuType === 'top').map(mapPublicMenuItem);
    const bottomMenu = items.filter((item) => item.menuType === 'bottom').map(mapPublicMenuItem);

    return res.status(200).json({
      status: true,
      error_type: '200',
      message: 'Menu List',
      // Providing both camelCase and snake_case for compatibility
      topMenu,
      top_menu: topMenu,
      bottomMenu,
      bottom_menu: bottomMenu
    });
  } catch (error) {
    return next(error);
  }
};

exports.createMenuItem = async (req, res, next) => {
  try {
    const {
      menuType,
      menuName,
      menuStatusId = 1,
      menuStatus = 'Active',
      menuActiveIcon,
      menuInActiveIcon,
      sortOrder = 1
    } = req.body;

    if (!['top', 'bottom'].includes(menuType)) {
      return res.status(400).json({ message: 'menuType must be top or bottom' });
    }
    if (!menuName || !menuActiveIcon || !menuInActiveIcon) {
      return res.status(400).json({ message: 'menuName, menuActiveIcon and menuInActiveIcon are required' });
    }

    const item = await MenuItem.create({
      menuType,
      menuName,
      menuStatusId,
      menuStatus,
      menuActiveIcon,
      menuInActiveIcon,
      sortOrder
    });

    return res.status(201).json(item);
  } catch (error) {
    return next(error);
  }
};

exports.getMenuItems = async (req, res, next) => {
  try {
    const where = {};
    if (req.query.menuType) {
      where.menuType = req.query.menuType;
    }

    const items = await MenuItem.findAll({
      where,
      order: [['menuType', 'ASC'], ['sortOrder', 'ASC'], ['id', 'ASC']]
    });
    return res.status(200).json(items);
  } catch (error) {
    return next(error);
  }
};

exports.updateMenuItem = async (req, res, next) => {
  try {
    const item = await MenuItem.findByPk(req.params.id);
    if (!item) {
      return res.status(404).json({ message: 'Menu item not found' });
    }

    const {
      menuType,
      menuName,
      menuStatusId,
      menuStatus,
      menuActiveIcon,
      menuInActiveIcon,
      sortOrder
    } = req.body;

    if (menuType && !['top', 'bottom'].includes(menuType)) {
      return res.status(400).json({ message: 'menuType must be top or bottom' });
    }

    await item.update({
      menuType: menuType ?? item.menuType,
      menuName: menuName ?? item.menuName,
      menuStatusId: menuStatusId ?? item.menuStatusId,
      menuStatus: menuStatus ?? item.menuStatus,
      menuActiveIcon: menuActiveIcon ?? item.menuActiveIcon,
      menuInActiveIcon: menuInActiveIcon ?? item.menuInActiveIcon,
      sortOrder: sortOrder ?? item.sortOrder
    });

    return res.status(200).json(item);
  } catch (error) {
    return next(error);
  }
};

exports.deleteMenuItem = async (req, res, next) => {
  try {
    const item = await MenuItem.findByPk(req.params.id);
    if (!item) {
      return res.status(404).json({ message: 'Menu item not found' });
    }
    await item.destroy();
    return res.status(200).json({ message: 'Menu item deleted' });
  } catch (error) {
    return next(error);
  }
};
