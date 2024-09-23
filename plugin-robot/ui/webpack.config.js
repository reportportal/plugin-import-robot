const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');
const ModuleFederationPlugin = require('webpack/lib/container/ModuleFederationPlugin');
const pjson = require('./package.json');
const ESLintWebpackPlugin = require('eslint-webpack-plugin');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');
const pluginName = pjson.name;

const config = {
  entry: path.resolve(__dirname, './src'),
  output: {
    path: path.resolve(__dirname, 'build/public'),
    filename: '[name].app.[contenthash:8].js',
    publicPath: 'auto',
    clean: true,
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.(sa|sc)ss$/,
        exclude: /node_modules/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              modules: {
                localIdentName: `${pluginName}_[name]__[local]--[hash:base64:5]`,
              },
            },
          },
          'sass-loader',
          {
            loader: 'sass-resources-loader',
            options: {
              resources: path.resolve(__dirname, './src/common/css/**/*.scss'),
            },
          },
        ],
      },
      {
        test: /\.svg$/,
        loader: 'svg-inline-loader',
      },
    ],
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.js', '.sass', '.scss', '.css'],
    alias: {
      components: path.resolve(__dirname, 'src/components'),
      constants: path.resolve(__dirname, 'src/constants'),
      icons: path.resolve(__dirname, 'src/icons'),
      hooks: path.resolve(__dirname, 'src/hooks'),
      utils: path.resolve(__dirname, 'src/utils'),
      analyticsEvents: path.resolve(__dirname, 'src/analyticsEvents'),
    },
  },
  externals: ['redux'],
  plugins: [
    new ForkTsCheckerWebpackPlugin(),
    new ESLintWebpackPlugin({
      context: './src',
      extensions: ['.scss', '.css', '.ts', '.tsx'],
      threads: true,
    }),
    new ModuleFederationPlugin({
      name: 'plugin_name',
      filename: `remoteEntity.js`,
      shared: {
        react: {
          import: 'react',
          shareKey: 'react',
          shareScope: 'default',
          singleton: true,
          requiredVersion: pjson.dependencies['react'],
        },
        'react-dom': {
          singleton: true,
          requiredVersion: pjson.dependencies['react-dom'],
        },
        'react-redux': {
          singleton: true,
          requiredVersion: pjson.dependencies['react-redux'],
        },
        'redux-form': {
          singleton: true,
          requiredVersion: pjson.dependencies['redux-form'],
        },
        moment: {
          singleton: true,
          requiredVersion: pjson.dependencies['moment'],
        },
        'react-tracking': {
          singleton: true,
          requiredVersion: pjson.dependencies['react-tracking'],
        },
        'html-react-parser': {
          singleton: true,
          requiredVersion: pjson.dependencies['html-react-parser'],
        },
        classnames: {
          singleton: true,
          requiredVersion: pjson.dependencies['classnames'],
        },
      },
      exposes: {
        './moduleName': './src/components/moduleName',
      },
    }),
    new CopyPlugin({
      patterns: [
        { from: path.resolve(__dirname, './src/metadata.json') },
        { from: path.resolve(__dirname, './src/plugin-icon.svg') },
      ],
    }),
  ],
};

module.exports = config;
